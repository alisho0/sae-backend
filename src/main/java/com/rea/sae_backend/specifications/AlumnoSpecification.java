package com.rea.sae_backend.specifications;

import org.springframework.data.jpa.domain.Specification;

import com.rea.sae_backend.models.Alumno;

public class AlumnoSpecification {
    public static Specification<Alumno> cumpleAsistencia(Boolean cumple) {
        return (root, query, criteriaBuilder) -> 
            cumple == null
                ? null
                : criteriaBuilder.equal(
                    root.get("cumpleAsistencia"),
                    cumple
                );
    }

    public static Specification<Alumno> dniEquals(String dni) {
        return (root, query, criteriaBuilder) -> 
            dni == null
                ? null
                : criteriaBuilder.equal(
                    root.get("dni"),
                    dni
                );
    }

    public static Specification<Alumno> escuelaIdEquals(Long escuelaId) {
        return (root, query, criteriaBuilder) -> 
            escuelaId == null
                ? null
                : criteriaBuilder.equal(
                    root.get("escuela").get("id"),
                    escuelaId
                );
    }
}
