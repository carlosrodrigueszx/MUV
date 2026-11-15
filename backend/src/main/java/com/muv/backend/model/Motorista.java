package com.muv.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "motorista")
@Data
public class Motorista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false, unique = true)
    private String cnh;

    @Column(name = "categoria_cnh")
    private String categoriaCnh;

    @Column(name = "usuario_id", unique = true)
    private Long usuarioId;
}