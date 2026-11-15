package com.muv.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class VinculoMotoristaId implements Serializable {
    @Column(name = "prestador_id")
    private Long prestadorId;

    @Column(name = "motorista_id")
    private Long motoristaId;
}
