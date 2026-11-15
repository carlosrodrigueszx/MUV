package com.muv.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "prestador_autonomo")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper = true)
public class PrestadorAutonomo extends Prestador {

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;
}