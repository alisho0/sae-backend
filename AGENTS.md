# Agent Instructions & Context: Spring Boot & Java 17 Project

## Tech Stack Overview
- **Language:** Java 17 (utilize modern features like Records, Text Blocks, and Switch Expressions where applicable).
- **Framework:** Spring Boot 3.x.
- **Build Tool:** Maven (using `mvnw` wrapper).
- **Database / ORM:** Spring Data JPA.
- **Testing:** JUnit 5, Mockito, and AssertJ.

---

## Essential Commands
Always use the Maven wrapper (`./mvnw` or `mvnw.cmd`) instead of global maven commands.

- **Build without tests:** `./mvnw clean package -DskipTests`
- **Run all tests:** `./mvnw test`
- **Run a specific test:** `./mvnw -Dtest=ClassNameTest test`
- **Start the application locally:** `./mvnw spring-boot:run`
- **Check dependency vulnerabilities:** `./mvnw dependency-check:check`

---

## Architecture & Style Guidelines

### 1. Layered Architecture
Strictly follow the clean layered architecture pattern:
- `controllers/` (REST Endpoints, Request/Response validation, Map to DTOs).
- `service/` (Business logic, @Transactional boundaries).
- `repositories/` (Spring Data JPA Interfaces).
- `models/` or `domain/` (JPA Entities).
- `dtos/` (Data Transfer Objects used for API input/output - use Java **Records**).

### 2. Code Preferences
- **Dependency Injection:** Always use **Constructor Injection** with `@RequiredArgsConstructor` and `private final class name`. Never use `@Autowired` on fields. Mark dependencies as `final`.
- **Lombok Usage:** Use `@RequiredArgsConstructor`, `@Getter`, and `@Setter`. Avoid `@Data` on JPA Entities to prevent circular dependencies in `toString()` or `hashCode()`.
- **Streams & Optionals:** Use functional-style streams and handle potential nulls with `Optional.orElseThrow()`.

---

## Forbidden Actions (What NOT to do)
- **Do not use standard exceptions:** Do not throw generic `RuntimeException`. Use or create custom business exceptions (e.g., `ResourceNotFoundException`).