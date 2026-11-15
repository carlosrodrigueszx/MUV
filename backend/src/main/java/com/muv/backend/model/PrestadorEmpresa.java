package com.muv.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "prestador_empresa")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper = true)
public class PrestadorEmpresa extends Prestador {

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia", nullable = false)
    private String nomeFantasia;
}