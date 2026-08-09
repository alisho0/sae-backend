package com.rea.sae_backend;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.rea.sae_backend.dtos.EscuelaRequestDto;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.services.EscuelaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SaeBackendApplicationTests {

	@Autowired
	private EscuelaService escuelaService;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldCreateSchoolWithDirectorUser() {
		EscuelaRequestDto dto = new EscuelaRequestDto();
		dto.setNombre("Escuela de prueba");
		dto.setCue("123456");
		dto.setUsuarioNombre("director-prueba-" + System.currentTimeMillis());
		dto.setUsuarioPassword("secret123");

		Escuela created = escuelaService.create(dto);

		assertNotNull(created);
		assertNotNull(created.getId());
		assertNotNull(created.getUsuario());
		assertNotNull(created.getUsuario().getId());
	}

}
