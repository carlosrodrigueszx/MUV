package com.muv.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "prestador")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
public class Prestador extends Usuario {

    @Column(name = "avaliacao_media", nullable = false, precision = 3, scale = 2)
    private Double avaliacaoMedia = 0.0;

    @Column(name = "total_avaliacoes", nullable = false)
    private Integer totalAvaliacoes = 0;
}