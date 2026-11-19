package com.muv.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "vinculo_motorista")
@Data
public class VinculoMotorista {
    @EmbeddedId
    private VinculoMotoristaId id = new VinculoMotoristaId();

    @ManyToOne
    @MapsId("prestadorId")
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    @ManyToOne
    @MapsId("motoristaId")
    @JoinColumn(name = "motorista_id")
    private Motorista motorista;

    @Column(name = "status_verificacao", nullable = false)
    private String statusVerificacao = "PENDENTE";

    @Column(name = "verificado_por_id")
    private Long verificadoPorId;

    @Column(name = "data_verificacao")
    private LocalDateTime dataVerificacao;

    @Column(name = "criado_em", insertable = false, updatable = false)
    private LocalDateTime criadoEm;
}