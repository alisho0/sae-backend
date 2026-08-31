package com.rea.sae_backend.specifications;

import com.rea.sae_backend.models.RegistroAsistencia;
import org.springframework.data.jpa.domain.Specification;

public class RegistroAsistenciaSpecification {

    public static Specification<RegistroAsistencia> periodoEquals(String periodo) {
        return (root, query, criteriaBuilder) ->
                periodo == null || periodo.isBlank()
                        ? null
                        : criteriaBuilder.equal(root.get("periodo"), periodo);
    }

    public static Specification<RegistroAsistencia> cumpleAsistencia(Boolean cumple) {
        return (root, query, criteriaBuilder) ->
                cumple == null
                        ? null
                        : criteriaBuilder.equal(root.get("cumpleAsistencia"), cumple);
    }

    public static Specification<RegistroAsistencia> dniEquals(String dni) {
        return (root, query, criteriaBuilder) ->
                dni == null || dni.isBlank()
                        ? null
                        : criteriaBuilder.equal(root.get("alumno").get("dni"), dni);
    }

    public static Specification<RegistroAsistencia> escuelaIdEquals(Long escuelaId) {
        return (root, query, criteriaBuilder) ->
                escuelaId == null
                        ? null
                        : criteriaBuilder.equal(root.get("alumno").get("escuela").get("id"), escuelaId);
    }
}