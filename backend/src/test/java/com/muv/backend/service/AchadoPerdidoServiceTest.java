package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.muv.backend.dto.RegistrarAchadoRequest;
import com.muv.backend.repository.AchadoPerdidoRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AchadoPerdidoServiceTest {
    private final AchadoPerdidoRepository repository = mock(AchadoPerdidoRepository.class);
    private final AchadoPerdidoService service = new AchadoPerdidoService(repository);
    private String png;

    @BeforeEach
    void prepararImagem() throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        png = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    @Test
    void registraItemComFreteOpcionalNoContextoAutorizado() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));
        when(repository.prestadorDoVeiculo(31, List.of(10L))).thenReturn(Optional.of(10L));
        when(repository.fretePertenceAoPrestador(51, 10)).thenReturn(true);

        long id = service.registrar(21, request(31L, 51L));

        assertThat(id).isZero();
        verify(repository).inserir(eq("Mochila e estojo"), eq(LocalDateTime.parse("2026-10-01T10:30:00")),
                eq(31L), eq(51L), eq(21L), any(byte[].class), eq("image/png"));
    }

    @Test
    void permiteRegistrarSemFrete() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));
        when(repository.prestadorDoVeiculo(31, List.of(10L))).thenReturn(Optional.of(10L));

        service.registrar(21, request(31L, null));

        verify(repository).inserir(eq("Mochila e estojo"), any(), eq(31L), eq(null), eq(21L),
                any(byte[].class), eq("image/png"));
    }

    @Test
    void rejeitaVeiculoOuFreteDeOutroPrestadorSemPersistir() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));
        when(repository.prestadorDoVeiculo(31, List.of(10L))).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.registrar(21, request(31L, null)));

        when(repository.prestadorDoVeiculo(32, List.of(10L))).thenReturn(Optional.of(10L));
        when(repository.fretePertenceAoPrestador(51, 10)).thenReturn(false);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.registrar(21, request(32L, 51L)));
        verify(repository, never()).inserir(anyString(), any(), anyLong(), nullable(Long.class), anyLong(),
                any(byte[].class), anyString());
    }

    @Test
    void exigeContextoDePrestadorOuMotoristaVinculado() {
        when(repository.prestadoresDoUsuario(99)).thenReturn(List.of());
        assertStatus(HttpStatus.FORBIDDEN, () -> service.listar(99));
    }

    @Test
    void limitaListagemAoContextoObtidoDaConta() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));

        service.listar(21);

        verify(repository).listar(List.of(10L));
    }

    @Test
    void confirmaDevolucaoSomenteParaItemEmGuardaDoPrestador() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));
        when(repository.situacaoParaAtualizar(61, List.of(10L))).thenReturn(Optional.of("EM_GUARDA"));

        service.confirmarEntrega(21, 61);

        verify(repository).marcarDevolvido(61);
    }

    @Test
    void permiteConfirmarDevolucaoDeItemLegadoEmCustodia() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));
        when(repository.situacaoParaAtualizar(63, List.of(10L))).thenReturn(Optional.of("EM_CUSTODIA"));

        service.confirmarEntrega(21, 63);

        verify(repository).marcarDevolvido(63);
    }

    @Test
    void rejeitaItemDeOutroPrestadorEItemJaDevolvido() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));
        when(repository.situacaoParaAtualizar(61, List.of(10L))).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND, () -> service.confirmarEntrega(21, 61));
        when(repository.situacaoParaAtualizar(62, List.of(10L))).thenReturn(Optional.of("DEVOLVIDO"));
        assertStatus(HttpStatus.CONFLICT, () -> service.confirmarEntrega(21, 62));
        verify(repository, never()).marcarDevolvido(62);
    }

    @Test
    void rejeitaFotoQueNaoSejaImagemValida() {
        when(repository.prestadoresDoUsuario(21)).thenReturn(List.of(10L));
        when(repository.prestadorDoVeiculo(31, List.of(10L))).thenReturn(Optional.of(10L));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.registrar(21,
                new RegistrarAchadoRequest("Mochila", LocalDateTime.now(), 31L, null,
                        "data:image/png;base64,ZmFrZQ==")));
        verify(repository, never()).inserir(anyString(), any(), anyLong(), nullable(Long.class), anyLong(),
                any(byte[].class), anyString());
    }

    private RegistrarAchadoRequest request(Long veiculo, Long frete) {
        return new RegistrarAchadoRequest(" Mochila e estojo ",
                LocalDateTime.parse("2026-10-01T10:30:00"), veiculo, frete, png);
    }

    private void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
        var error = assertThrows(ResponseStatusException.class, action);
        assertThat(error.getStatusCode()).isEqualTo(expected);
    }
}
