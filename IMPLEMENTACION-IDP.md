# Plan de Implementación: Integración de IDP (Keycloak)

> Objetivo: reemplazar el JWT local (jjwt + HS256) por autenticación/autorización
> delegada a un Identity Provider (IDP), conservando la tabla `usuarios` como
> registro de dominio (**Opción A**).

## Estado de implementación

- [x] **Fase 0 — Control de daños** (eliminado `/auth/register` público; agregado `spring-boot-starter-oauth2-resource-server` para compilar).
- [x] **Fase 1 — Provisión de Keycloak** (`docker-compose.yml`, `infra/realm-export.json`).
- [x] **Fase 2 — Backend resource server (OIDC)**.
- [x] **Fase 3 — Mapeo de identidad y aislamiento por escuela**.
- [ ] **Fase 4 — Frontend OIDC (PKCE)**.
- [ ] **Fase 5 — Migración de los usuarios existentes**.
- [ ] **Fase 6 — Endurecimiento, limpieza y pruebas**.

---

## 1. Contexto y estado actual

**Backend:** Spring Boot 4.1 / Java 17 / MySQL / Spring Security.
**Frontend:** React + Vite + Redux Toolkit (proyecto `../sae-front`).

### Flujo actual (JWT local)

| Pieza | Archivo | Rol |
|---|---|---|
| Login `POST /auth/login` | `AuthController.java` | valida credenciales → BD (`usuarios`) → emite JWT |
| Registro público `POST /auth/register` | `AuthController.java` | crea usuario con rol **arbitrario** (vulnerabilidad) |
| Emisión/firma JWT | `JwtService.java` | HS256 con `jwt.secret` (sae-backend `application.yaml`) |
| Validación por request | `JwtAuthenticationFilter.java` | decodifica token → busca usuario en BD → `SecurityContext` |
| Carga de usuario | `CustomUserDetailsService` / `CustomUserDetails` | `ROLE_<ADMIN\|DIRECTOR>` + `escuela_id` |
| Autorización por URL | `SecurityConfig.java` (`requestMatchers`) | `/usuarios/**`, `/configuracion/**` → ADMIN; `/escuelas/**`, `/alumnos/**` → ADMIN/DIRECTOR |
| Decodificación client-side | `sae-front`: `store/slices/authSlice.js`, `utils/auth.js` | role y `escuela_id` desde el payload JWT |

### Artefactos de Keycloak ya presentes (incompletos)

- `security/KeycloakJwtAuthenticationConverter.java` — existe pero **no está conectado** a Spring Security.
- `TestController.testKeycloak()` — apunta a `http://localhost:8080/realms/sae_realm/...`.

---

## 2. Análisis de riesgos del JWT local y de la arquitectura actual

| # | Riesgo | Impacto | Severidad |
|---|---|---|---|
| 1 | `/auth/register` público acepta `role` arbitrario | Escalada a ADMIN sin autenticación | **Crítico** |
| 2 | Secreto HS256 compartido, sin rotación, sin JWKS | Token forjable si se filtra el secreto | Alto |
| 3 | Token sin estándar OIDC (`iss`, `aud`), sin refresh ni revocación | Sesiones no gestionables, sin logout real | Alto |
| 4 | Token en `localStorage` (frontend) | Exposición por XSS | Alto |
| 5 | `escuela_id` no se valida en servidor | Un DIRECTOR puede operar datos de otras escuelas | **Crítico** (autorización) |
| 6 | Contraseñas auto-gestionadas, sin MFA/políticas | Credenciales débiles, sin auditoría central | Medio |
| 7 | `jwt.secret` con valor por defecto en YAML | Deploy inseguro si no se sobrescribe | Medio |

---

## 3. Decisión de arquitectura

### 3.1 IDP elegido: Keycloak

Para una carga de ~50 usuarios:

- **Gratis y self-hosted** (Docker), sin costo por usuario (un SaaS como Okta cobra por user).
- OIDC Authorization Code + PKCE, roles de realm, MFA (TOTP), políticas de contraseña, UI de administración.
- Ya hay código Keycloak en el repo (reduce esfuerzo).
- Despliegue liviano: docker-compose junto al backend.

**Alternativas:** Zitadel (moderno, tier libre), Auth0 (SaaS, cero infraestructura).

### 3.2 Modelo de datos: Opción A (seleccionada)

Se mantiene la tabla `usuarios` como **registro de dominio** (vínculo a `escuela`)
y se agrega `keycloak_subject` (UUID del usuario en Keycloak) como clave estable.
El **rol y la autenticación provienen del token**; el rol guardado en BD deja de
ser fuente de verdad (se conserva solo por compatibilidad durante la transición).

```
Keycloak (identidad + roles + MFA)
        │  JWT (issuer = Keycloak, realm_access.roles, claim escuela_id, sub)
        ▼
   Backend (Resource Server)
        │  sub  ──►  usuarios.keycloak_subject  ──►  escuela (dominio)
        ▼
    Frontend (OIDC PKCE, sin tokens en localStorage)
```

---

## 4. Modelo de datos (Implementación)

### 4.1 Migración SQL

```sql
ALTER TABLE usuarios
    ADD COLUMN keycloak_subject VARCHAR(64) NULL,
    ADD CONSTRAINT uk_usuarios_keycloak_subject UNIQUE (keycloak_subject);

CREATE INDEX idx_usuarios_keycloak_subject ON usuarios (keycloak_subject);
```

> Con `spring.jpa.hibernate.ddl-auto: update`, basta con declarar el campo en la
> entidad; el SQL anterior es la vía explícita para entornos productivos.

### 4.2 Entidad `Usuario`

```java
@Entity
@Table(name = "usuarios")
@Data
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "keycloak_subject", unique = true)
    private String keycloakSubject;   // sub / UUID del usuario en Keycloak

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;          // queda obsoleto post-migración

    @OneToOne
    @JoinColumn(name = "escuela_id")
    @JsonIgnoreProperties("usuario")
    private Escuela escuela;

    @Enumerated(EnumType.STRING)
    private Role role;                // deprecated; el rol real proviene del token
}
```

### 4.3 Repositorio

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByKeycloakSubject(String keycloakSubject);
    boolean existsByUsername(String username);
}
```

### 4.4 Servicio de usuario de dominio

```java
@Service
public class UsuarioService {

    // ... métodos existentes ...

    public Optional<Usuario> findByKeycloakSubject(String subject) {
        return usuarioRepository.findByKeycloakSubject(subject);
    }

    /** Vincula un registro de dominio al identity de Keycloak. */
    public void asignarSubject(String username, String subject) {
        findByUsername(username).ifPresent(u -> {
            u.setKeycloakSubject(subject);
            usuarioRepository.save(u);
        });
    }
}
```

---

## 5. Fases de implementación

### Fase 0 — Control de daños inmediato (previo a la migración)

1. **Eliminar `/auth/register`** del `permitAll()` de `SecurityConfig`
   (o borrar el endpoint del `AuthController`). La creación de usuarios pasa a
   hacerse por el ADM en Keycloak y, transitoriamente, vía `/usuarios/**`.
2. Verificar que no existan usuarios ADMIN creados vía el endpoint público; de
   existir, rotarlos.
3. (Opcional) Rotar `JWT_SECRET` y forzar cambio de contraseñas antes del corte.

**Entregable:** ninguna superficie de registro público; solo `ADMIN` crea identidades.

---

### Fase 1 — Provisión de Keycloak

#### 1.1 Docker Compose

```yaml
# docker-compose.yml (raíz del repo o carpeta /infra)
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.1
    command: start-dev
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: ${KC_ADMIN_PASSWORD}
      # En producción: postgresql/mysql gestionado
    ports:
      - "8081:8080"        # 8080 lo ocupa el backend
    volumes:
      - ./infra/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro
```

> El realm se importa automáticamente en el primer arranque con `start-dev`
> (opcional: `KC_IMPORT_ENABLED` en versiones productivas).

#### 1.2 Configuración en el realm `sae_realm`

| Elemento | Valor |
|---|---|
| Realm | `sae_realm` |
| Client **frontend** | `sae-front` — flujo `public`, Authorization Code + PKCE, redirect `http://localhost:5173` |
| Client **backend** (útil para refresh/confidential) | `sae-backend` — secret en variable de entorno |
| Roles de realm | `ADMIN`, `DIRECTOR` |
| Atributo de usuario | `escuela_id` (numérico) — expuesto al token vía **Client Scope Mapper → User Attribute** con claim `escuela_id` |
| Redirect URIs / Web origins | entorno local: `http://localhost:5173/*` |

#### 1.3 `realm-export.json` (extracto — roles + mapper)

```json
{
  "realm": "sae_realm",
  "enabled": true,
  "roles": {
    "realm": [
      { "name": "ADMIN" },
      { "name": "DIRECTOR" }
    ]
  },
  "clients": [
    {
      "clientId": "sae-front",
      "publicClient": true,
      "standardFlowEnabled": true,
      "redirectUris": ["http://localhost:5173/*"],
      "webOrigins": ["http://localhost:5173"],
      "protocolMappers": [
        {
          "name": "escuela_id",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-attribute-mapper",
          "config": {
            "user.attribute": "escuela_id",
            "claim.name": "escuela_id",
            "jsonType.label": "String",
            "id.token.claim": "true",
            "access.token.claim": "true"
          }
        }
      ]
    }
  ]
}
```

#### 1.4 Políticas (seguridad)

- Política de contraseñas: longitud ≥ 10, al menos una mayúscula, número y símbolo; expiración 90 días.
- Activación de MFA (TOTP) para el rol `ADMIN`.
- Login Theme por defecto; deshabilitar "Register" público en el realm (evitar que el IDP ofrezca auto-registro).

---

### Fase 2 — Backend como Resource Server (OIDC)

#### 2.1 Dependencia Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

> Permite dejar de usar `spring-boot-starter-security` "crudo" con filtro propio;
> el starter trae el soporte JWT (validación de firma contra JWKS del issuer).

#### 2.2 `application.yaml`

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KC_ISSUER_URI:http://localhost:8081/realms/sae_realm}
          # jwk-set-uri se deriva automáticamente del issuer-uri; opcional explícito:
          # jwk-set-uri: ${KC_JWKS_URI:http://localhost:8081/realms/sae_realm/protocol/openid-connect/certs}
          audiences: sae-front, sae-backend

# (se elimina el bloque jwt.local posteriormente)
jwt:
  secret: ${JWT_SECRET:...}   # ← solo durante la transición (Fase 0/2); se retira en Fase 6
  expiration-ms: 86400000
```

#### 2.3 `SecurityConfig` (nuevo)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Durante la transición se mantiene /auth/login (ver Fase 6)
                .requestMatchers("/auth/login", "/error").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/configuracion/**").hasRole("ADMIN")
                .requestMatchers("/escuelas/**").hasAnyRole("ADMIN", "DIRECTOR")
                .requestMatchers("/alumnos/**").hasAnyRole("ADMIN", "DIRECTOR")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)));
        return http.build();
    }
}
```

> **Nota (implementado):** `JwtAuthenticationFilter` se eliminó en esta fase (no en la 6).
> Como el filtro era `@Component`, Spring Boot lo registraba también como *servlet filter*,
> por lo que un token OIDC habría provocado un error 500 antes de llegar a Spring Security.
> Su validación quedó cubierta por el decodificador dual de `JwtResourceServerConfig`
> (Keycloak RS256 + legacy HS256). El bean `AuthenticationManager`
> (`configuration.getAuthenticationManager()`) se **conserva**: en Spring Security 7
> `AuthenticationConfiguration` no lo publica como bean y `AuthController` lo inyecta.
> `JwtService`, `CustomUserDetailsService` y `CustomUserDetails` se conservan por el
> login legacy (`/auth/login`).

> Se eliminan: `JwtAuthenticationFilter`, `JwtService` (jjwt), `DaoAuthenticationProvider`
> y `CustomUserDetailsService` pasan a modo "legacy" (ver Fase 6). El `PasswordEncoder`
> puede conservarse solo para migración/legacy.

#### 2.4 `KeycloakJwtAuthenticationConverter` (corregido)

```java
@Component
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Set<String> APP_ROLES = Set.of("ADMIN", "DIRECTOR");

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Solo roles de la aplicación (evitar offline_access, uma_authorization, etc.)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            roles.stream()
                .map(String::valueOf)
                .filter(APP_ROLES::contains)
                .forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        // Atributos de dominio accesibles para scoping
        var details = Map.of(
            "escuela_id", jwt.getClaim("escuela_id")
        );
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject(), details);
    }
}
```

> Privilegie `new JwtAuthenticationToken(jwt, authorities, name, details)` para que
> los claims queden disponibles en el principal; o bien inyecte `Jwt` directamente
> en los controllers con `@AuthenticationPrincipal`.

---

### Fase 3 — Mapeo de identidad y aislamiento por escuela

#### 3.1 Servicio `CurrentUserService` (resuelve identidad de dominio)

```java
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UsuarioService usuarioService;

    public String getSubject() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        throw new AccessDeniedException("No autenticado con token OIDC");
    }

    public Long getEscuelaId() {
        return getAuthentication()
            .map(auth -> ((JwtAuthenticationToken) auth).getToken().getClaim("escuela_id"))
            .map(this::toLong)
            .orElseThrow(() -> new AccessDeniedException("Claim escuela_id ausente"));
    }

    public Set<String> getRoles() {
        return getAuthentication()
            .map(auth -> auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet()))
            .orElseGet(Set::of);
    }

    public boolean isAdmin() { return getRoles().contains("ADMIN"); }

    /** Carga el registro de dominio vinculado al subject (para escuela u otros datos). */
    public Usuario getUsuarioDominio() {
        return usuarioService.findByKeycloakSubject(getSubject())
            .orElseThrow(() -> new AccessDeniedException("Usuario de dominio no encontrado"));
    }

    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        return null;
    }
}
```

#### 3.2 Protección por método (`@PreAuthorize`) + scoping en `AlumnoController`

```java
@RestController
@RequestMapping("/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final CurrentUserService currentUser;

    @GetMapping("/escuela/{id}")
    @PreAuthorize("hasRole('ADMIN') or (#id == @currentUserService.getEscuelaId())")
    public List<AlumnoResponseDto> listByEscuela(@PathVariable Long id,
                                                 @RequestParam(required = false) String periodo) {
        // ... igual que antes ...
    }

    @GetMapping
    public Page<AlumnoResponseDto> list(@RequestParam(...) Long escuelaId, ...) {
        // Si el usuario es DIRECTOR, su escuela SIEMPRE se impone desde el token:
        Long escuelaEfectiva = currentUser.isAdmin() ? escuelaId : currentUser.getEscuelaId();
        return alumnoService.findAll(pageable, cumpleAsistencia, dni, escuelaEfectiva, periodo)
            .map(AlumnoResponseDto::fromEntity);
    }
}
```

> Aplicar el mismo patrón en `EscuelaController`, `ReporteController` y
> `AlumnoService` (crear/update/importar): si el rol es `DIRECTOR`, la escuela se
> toma del token y se ignora el valor del body/query. Así se cierra el riesgo #5.

#### 3.3 Proyección en `UsuarioController`/`UsuarioResponseDto`

- `UsuarioResponseDto` agrega `keycloakSubject` (o el `username` del IDP).
- `/usuarios/**` sigue siendo ADMIN-only. El ADMIN ve/crea usuarios y los vincula
  con su `keycloakSubject` (campo cargado por el endpoint de admin, no editable por el usuario).

> **Nota (implementado):**
> - `Usuario` declara `@Column(name = "keycloak_subject", unique = true)`
>   (se aplica con `ddl-auto: update` en dev; el DDL explícito va al deploy).
> - Nuevo `CurrentUserService` en `security/` (`getSubject`, `getEscuelaId` con
>   Number/String, `getRoles`, `isAdmin`, `isEscuelaAccesible`, `getUsuarioDominio`).
> - `AlumnoController`: `@PreAuthorize` en `GET /escuela/{id}`; en `list/create/
>   update/cargar-excel` la escuela del DIRECTOR se impone desde el token (ignora
>   body/query); `getById/asistencia/delete` validan que el registro pertenezca a
>   la escuela del token (403 si no).
> - `EscuelaController`: `GET/PUT /{id}` y `cerrar-asistencia` → ADMIN o propia
>   escuela; `POST`, `DELETE` y `cargar-excel` → solo ADMIN; `list()` devuelve la
>   propia escuela para DIRECTOR.
> - `ReporteController`: `GET /escuela/{id}` → ADMIN o propia escuela; `GET /escuelas`
>   → solo ADMIN.
> - `AlumnoService.cargarAlumnosDesdeExcel` acepta `escuelaIdRestringida` (omitida
>   por un DIRECTOR, no es de su escuela).
> - `UsuarioResponseDto` expone `keycloakSubject`; la BD conserva `role` como dato
>   de dominio (deprecado, el rol real proviene del token).

---

### Fase 4 — Frontend con OIDC (PKCE)

#### 4.1 Dependencia

```bash
pnpm add keycloak-js
```

#### 4.2 Inicialización (reemplaza `authSlice`/`LoginPage`)

```js
// src/services/keycloak.js
import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081',
  realm: 'sae_realm',
  clientId: 'sae-front',
})

export default keycloak
```

```js
// src/main.jsx (o un AuthProvider)
import keycloak from './services/keycloak'

keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', flow: 'standard' })
  .then((authenticated) => {
    keycloak.onTokenExpired = () => keycloak.updateToken(30)
    // conectar el estado de Redux con keycloak.authenticated / keycloak.token
  })
```

#### 4.3 Envío del token en `api.js` (desde Keycloak, no `localStorage`)

```js
api.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    if (keycloak.isTokenExpired()) await keycloak.updateToken(5)
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})
```

#### 4.4 Consumo de claims (reemplaza `decodeToken` / `localStorage`)

```js
// utils/auth.js — ahora lee del perfil de Keycloak / JWT
export const getTokenRoles = (tokenAccess) => {
  const roles = tokenAccess?.realm_access?.roles ?? tokenAccess?.realm_access?.roles ?? []
  if (roles.some((r) => /admin|super/i.test(r))) return 'ADMIN'
  if (roles.some((r) => /director/i.test(r))) return 'DIRECTOR'
  return 'USER'
}

export const getEscuelaIdFromToken = (tokenAccess) =>
  tokenAccess?.escuela_id ?? tokenAccess?.escuelaId ?? null
```

- `authSlice` pasa a derivarse de `keycloak.tokenParsed`; `logout` llama a
  `keycloak.logout()` (elimina el `localStorage` del token).
- `LogoutButton` invoca `keycloak.logout({ redirectUri: 'http://localhost:5173/login' })`.

---

### Fase 5 — Migración de los usuarios existentes (~50)

#### 5.1 Estrategia

Keycloak **no importa hashes BCrypt** de terceros de forma directa para login.
Para ~50 usuarios se recomienda:

1. Exportar `usuarios` (username, escuela_id, rol) desde MySQL.
2. Crear en Keycloak (API admin o `realm-export.json`) con:
   - `username` = username actual
   - **password temporal** aleatoria + atributo `requiredActions: ["UPDATE_PASSWORD"]`
   - rol `ADMIN`/`DIRECTOR` según BD
   - atributo `escuela_id`
3. Devolver/entregar las credenciales temporales (mail/cartel) y forzar cambio en el primer login.

#### 5.2 Script de migración (Keycloak Admin REST + backend)

```bash
# 1) Token de admin (realme direct grants / service account)
TOKEN=$(curl -s -X POST http://localhost:8081/realms/master/protocol/openid-connect/token \
  -d "grant_type=password" -d "client_id=admin-cli" \
  -d "username=admin" -d "password=$KC_ADMIN_PASSWORD" | jq -r .access_token)

# 2) Por cada usuario exportado de la BD:
curl -s -X POST "http://localhost:8081/admin/realms/sae_realm/users" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "username": "<username>",
    "enabled": true,
    "emailVerified": false,
    "requiredActions": ["UPDATE_PASSWORD"],
    "attributes": { "escuela_id": ["<id_escuela>"] },
    "realmRoles": ["<ADMIN|DIRECTOR>"],
    "credentials": [ { "type": "password", "value": "<password_temporal>", "temporary": true } ]
  }'
```

> Alternativa reproducible: generar el `realm-export.json` completo (incluye users)
> y usar la consola de admin para password temporal + `UPDATE_PASSWORD`.

#### 5.3 Backfill del `keycloak_subject` en BD

```java
// Endpoint/CLI de migración: una vez creados los usuarios en Keycloak,
// se obtiene el sub y se asocia al registro de dominio.
@Service
public class MigracionIdpService {
    public void vincular(String username, String keycloakUuid) {
        usuarioService.asignarSubject(username, keycloakUuid);
    }
}
```

```bash
# Psicosis opcional: consultar el sub por username
curl -s "http://localhost:8081/admin/realms/sae_realm/users?username=<username>" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id'
```

> **Criterio de corte (cutover):** cuando el 100% de los usuarios tenga
> `keycloak_subject` cargado y los usuarios hayan iniciado sesión vía Keycloak,
> se elimina `/auth/login` legacy y el bloque `jwt.*` de configuración.

---

### Fase 6 — Endurecimiento, limpieza y pruebas

#### 6.1 Limpieza en backend

- Eliminar dependencias `jjwt-*` del `pom.xml`.
- Eliminar `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`,
  `CustomUserDetails`, `AuthenticationProvider` y el endpoint `POST /auth/login`.
- Eliminar el bloque `jwt:` de `application.yaml`.
- Eliminar `TestController.testKeycloak()` (o todo `TestController` de prueba).
- Eliminar la columna `password` de `usuarios` (opcional, tras confirmar).
  Sugerencia: mantener por unos releases para rollback.
- `AuthController` se reduce a logout/health de sesión o se elimina.

#### 6.2 Seguridad adicional

- Login en realm con `session idle` y `session max` de 30/480 min.
- Secretos en variables de entorno (`KC_ADMIN_PASSWORD`, `DB_*`, `JWT_SECRET` legacy).
- Eventos de auditoría de Keycloak activados (login, logout, admin actions).
- HTTPS en producción (seguridad de los tokens y del IDP).

#### 6.3 Pruebas

```xml
<!-- ya presente: spring-boot-starter-security-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Pruebas con JWT de prueba emitidos contra Keycloak (o `JwtDecoder` mockeado en unit):

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired MockMvc mockMvc;

    @Test
    void directorNoAccedeAUscenioaDeOtro() throws Exception {
        String token = obtenerAccessToken("director_escuela_1", "pass");
        mockMvc.perform(get("/alumnos/escuela/2")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());   // o 404 según política
    }

    @Test
    void sinTokenQueda401() throws Exception {
        mockMvc.perform(get("/alumnos")).andExpect(status().isUnauthorized());
    }
}
```

#### 6.4 Swagger/OpenAPI

`OpenApiConfig` mantiene `bearerAuth` (correcto para OIDC). Opcionalmente agregar
`securitySchemes` con `OAuth2` + flujo authorization_code para testear desde Swagger UI.

---

## 6. Resumen de archivos afectados

| Archivo | Cambio |
|---|---|
| `pom.xml` | + `spring-boot-starter-oauth2-resource-server`; − jjwt (Fase 6) |
| `application.yaml` | + issuer-uri/audiences; − `jwt.*` (Fase 6) |
| `SecurityConfig.java` | `.oauth2ResourceServer(...)`; sin filtro local |
| `KeycloakJwtAuthenticationConverter.java` | filtrar solo roles de app + details |
| `JwtService/Filter/UserDetails*` | eliminados (Fase 6) |
| `models/Usuario.java`, `UsuarioRepository`, `UsuarioService` | + `keycloakSubject` |
| `controllers/AuthController.java` | solo legacy/transición |
| `controllers/AlumnoController.java` y afines | `@PreAuthorize` + scoping por escuela |
| `security/CurrentUserService.java` | nuevo helper de claims |
| `sae-front`: `keycloak.js`, `authSlice`, `LoginPage`, `api.js`, `LogoutButton` | OIDC PKCE |
| `docker-compose.yml`, `infra/realm-export.json` | Keycloak + realm |

---

## 7. Plan de corte (cutover) y rollback

**Secuencia sugerida:**
1. Fase 0 → 1 (Keycloak corriendo, realm listo).
2. Fase 2 en paralelo con Fase 5 (backend acepta tokens Keycloak; IDP + backfill de `keycloak_subject`).
3. Fase 4 frontend OIDC (los usuarios ya loguean por Keycloak).
4. Fase 6 limpieza (un release después del corte).

**Rollback:** mientras `/auth/login` legacy y `jwt.*` sigan presentes, basta con
volver al `SecurityFilterChain` anterior y a jjwt (todo queda 1 commit atrás).

---

## 8. Estimación (referencial)

| Fase | Esfuerzo |
|---|---|
| Fase 0 (cierre de registro) | 0.5 h |
| Fase 1 (Keycloak + realm + roles) | 3–6 h |
| Fase 2 (resource server) | 3–6 h |
| Fase 3 (mapeo + scoping) | 4–8 h |
| Fase 4 (front OIDC) | 6–12 h |
| Fase 5 (migración ~50 usuarios) | 3–6 h |
| Fase 6 (limpieza + pruebas) | 4–8 h |
| **Total** | **~1 semana (tiempo parcial)** |