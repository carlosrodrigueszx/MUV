package com.muv.backend.service;

import com.muv.backend.dto.AchadoPerdidoResponse;
import com.muv.backend.dto.AchadoPerdidoOpcoesResponse;
import com.muv.backend.dto.RegistrarAchadoRequest;
import com.muv.backend.repository.AchadoPerdidoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AchadoPerdidoService {
    private final AchadoPerdidoRepository repository;

    public AchadoPerdidoService(AchadoPerdidoRepository repository) {
        this.repository = repository;
    }

    public List<AchadoPerdidoResponse> listar(long usuarioId) {
        return repository.listar(prestadoresDoUsuario(usuarioId));
    }

    public AchadoPerdidoOpcoesResponse opcoes(long usuarioId) {
        return repository.opcoes(prestadoresDoUsuario(usuarioId));
    }

    @Transactional
    public long registrar(long usuarioId, RegistrarAchadoRequest request) {
        List<Long> prestadores = prestadoresDoUsuario(usuarioId);
        long prestadorId = repository.prestadorDoVeiculo(request.veiculoId(), prestadores)
                .orElseThrow(() -> naoEncontrado("Veículo não encontrado."));
        if (request.freteId() != null && !repository.fretePertenceAoPrestador(request.freteId(), prestadorId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O frete relacionado precisa pertencer ao prestador responsável pelo veículo.");

        var foto = AchadoPerdidoPhoto.parse(request.foto());
        return repository.inserir(request.descricao().trim(), request.dataEncontrado(),
                request.veiculoId(), request.freteId(), usuarioId, foto.bytes(), foto.mimeType());
    }

    @Transactional
    public void confirmarEntrega(long usuarioId, long itemId) {
        List<Long> prestadores = prestadoresDoUsuario(usuarioId);
        String status = repository.situacaoParaAtualizar(itemId, prestadores)
                .orElseThrow(() -> naoEncontrado("Objeto não encontrado."));
        if (!"EM_GUARDA".equals(status) && !"EM_CUSTODIA".equals(status))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente objetos ainda em guarda podem ser marcados como devolvidos.");
        repository.marcarDevolvido(itemId);
    }

    private List<Long> prestadoresDoUsuario(long usuarioId) {
        List<Long> prestadores = repository.prestadoresDoUsuario(usuarioId);
        if (prestadores.isEmpty())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta operação é restrita ao prestador responsável e aos seus motoristas vinculados.");
        return prestadores;
    }

    private static ResponseStatusException naoEncontrado(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
