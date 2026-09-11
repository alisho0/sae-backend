package com.rea.sae_backend.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TestController {
    @GetMapping("/publico")
    public String publicEndpoint() {
        return "PUBLIC OK";
    }

    @GetMapping("/private")
    public String privateEndpoint() {
        return "PRIVATE OK";
    }
    @GetMapping("/test-keycloak")
    public String testKeycloak() {
        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.getForObject(
                "http://localhost:8080/realms/sae_realm/protocol/openid-connect/certs",
                String.class
        );
    }
}
