package com.muv.backend.service;

import com.muv.backend.model.Motorista;
import com.muv.backend.model.Prestador;
import com.muv.backend.model.VinculoMotorista;
import com.muv.backend.model.VinculoMotoristaId;
import com.muv.backend.repository.MotoristaRepository;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.repository.PrestadorEmpresaRepository;
import com.muv.backend.model.PrestadorEmpresa;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.muv.backend.repository.PrestadorRepository;
import com.muv.backend.repository.VinculoMotoristaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverLinkingServiceTest {

    @Mock
    private MotoristaRepository motoristaRepository;

    @Mock
    private VinculoMotoristaRepository vinculoMotoristaRepository;

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock private AuthRepository accounts;
    @Mock private DriverAccountService driverAccounts;
    @Mock private PrestadorEmpresaRepository empresaRepository;

    @InjectMocks
    private DriverLinkingService driverLinkingService;

    private Prestador prestadorMock;
    private Motorista motoristaMock;
    private VinculoMotorista vinculoMock;

    @BeforeEach
    void setUp() {
        prestadorMock = new Prestador();
        prestadorMock.setId(1L);

        motoristaMock = new Motorista();
        motoristaMock.setId(10L);
        motoristaMock.setNome("Carlos Silva");
        motoristaMock.setTelefone("85999999999");
        motoristaMock.setCnh("12345678900");
        motoristaMock.setCategoriaCnh("B");

        VinculoMotoristaId vinculoId = new VinculoMotoristaId();
        vinculoId.setPrestadorId(1L);
        vinculoId.setMotoristaId(10L);

        vinculoMock = new VinculoMotorista();
        vinculoMock.setId(vinculoId);
        vinculoMock.setPrestador(prestadorMock);
        vinculoMock.setMotorista(motoristaMock);
        vinculoMock.setStatusVerificacao("PENDENTE");
    }

    @Test
    void deveListarMotoristasPorPrestadorComSucesso() {
        when(vinculoMotoristaRepository.findByIdPrestadorId(1L)).thenReturn(Collections.singletonList(vinculoMock));

        List<VinculoMotorista> resultado = driverLinkingService.listarMotoristasPorPrestador(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("PENDENTE", resultado.get(0).getStatusVerificacao());
        verify(vinculoMotoristaRepository, times(1)).findByIdPrestadorId(1L);
    }

    @Test
    void deveCadastrarEVincularMotoristaComSucesso() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestadorMock));
        when(driverAccounts.cadastrar(any())).thenReturn(motoristaMock);
        when(vinculoMotoristaRepository.save(any(VinculoMotorista.class))).thenReturn(vinculoMock);

        VinculoMotorista resultado = driverLinkingService.cadastrarEVincularMotorista(1L, cadastro());

        assertNotNull(resultado);
        assertEquals("PENDENTE", resultado.getStatusVerificacao());
        verify(prestadorRepository, times(1)).findById(1L);
        verify(driverAccounts).cadastrar(any());
        verify(vinculoMotoristaRepository, times(1)).save(any(VinculoMotorista.class));
    }

    @Test
    void deveLancarExcecaoAoCadastrarPrestadorInexistente() {
        when(prestadorRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            driverLinkingService.cadastrarEVincularMotorista(99L, cadastro());
        });

        assertTrue(exception.getMessage().contains("Prestador não encontrado."));
        verify(motoristaRepository, never()).save(any(Motorista.class));
    }

    @Test
    void deveAtualizarStatusVerificacaoComSucesso() {
        when(vinculoMotoristaRepository.findByIdPrestadorIdAndIdMotoristaId(1L, 10L))
                .thenReturn(Optional.of(vinculoMock));
        when(vinculoMotoristaRepository.save(any(VinculoMotorista.class))).thenReturn(vinculoMock);

        VinculoMotorista resultado = driverLinkingService.atualizarVerificacao(1L, 10L, "VERIFICADO", 1L);

        assertNotNull(resultado);
        assertEquals("VERIFICADO", resultado.getStatusVerificacao());
        assertNotNull(resultado.getDataVerificacao());
        assertEquals(1L, resultado.getVerificadoPorId());
        verify(vinculoMotoristaRepository, times(1)).findByIdPrestadorIdAndIdMotoristaId(1L, 10L);
        verify(vinculoMotoristaRepository, times(1)).save(any(VinculoMotorista.class));
    }

    @Test
    void deveLancarExcecaoAoAtualizarVinculoInexistente() {
        when(vinculoMotoristaRepository.findByIdPrestadorIdAndIdMotoristaId(1L, 99L))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            driverLinkingService.atualizarVerificacao(1L, 99L, "VERIFICADO", 1L);
        });

        assertTrue(exception.getMessage().contains("Vínculo não encontrado entre o prestador e o motorista."));
        verify(vinculoMotoristaRepository, never()).save(any(VinculoMotorista.class));
    }

    @Test
    void deveRejeitarCnhDuplicadaSemSalvar() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestadorMock));
        when(motoristaRepository.existsByCnh(motoristaMock.getCnh())).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> driverLinkingService.cadastrarEVincularMotorista(1L, cadastro())).getStatusCode());
        verify(motoristaRepository, never()).save(any());
        verify(vinculoMotoristaRepository, never()).save(any());
    }

    @Test
    void deveVincularMotoristaExistenteSemDuplicarCadastro() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestadorMock));
        when(motoristaRepository.findById(10L)).thenReturn(Optional.of(motoristaMock));
        when(vinculoMotoristaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var resultado = driverLinkingService.vincularMotorista(1L, 10L);
        assertEquals(10L, resultado.getId().getMotoristaId());
        assertEquals("PENDENTE", resultado.getStatusVerificacao());
        assertNull(resultado.getVerificadoPorId());
        verify(motoristaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarVinculoDuplicadoSemApagarVerificacao() {
        when(prestadorRepository.findById(1L)).thenReturn(Optional.of(prestadorMock));
        when(motoristaRepository.findById(10L)).thenReturn(Optional.of(motoristaMock));
        when(vinculoMotoristaRepository.findByIdPrestadorIdAndIdMotoristaId(1L, 10L)).thenReturn(Optional.of(vinculoMock));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> driverLinkingService.vincularMotorista(1L, 10L)).getStatusCode());
        verify(vinculoMotoristaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarStatusInvalidoSemAlterarVinculo() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> driverLinkingService.atualizarVerificacao(1L, 10L, "INVALIDO", 1L)).getStatusCode());
        verifyNoInteractions(vinculoMotoristaRepository);
    }

    @Test
    void deveImpedirAcessoAOutraOrganizacao() {
        when(accounts.findAccount("gestor@example.test")).thenReturn(Optional.of(
                new AuthRepository.Account(1L, "Gestor", "gestor@example.test", "hash")));
        PrestadorEmpresa empresa = new PrestadorEmpresa();
        empresa.setId(1L);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> driverLinkingService.autorizar(2L, "gestor@example.test")).getStatusCode());
        verifyNoInteractions(motoristaRepository, vinculoMotoristaRepository);
    }

    @Test
    void deveManterVerificacaoIndependentePorOrganizacao() {
        when(vinculoMotoristaRepository.findByIdPrestadorIdAndIdMotoristaId(1L, 10L)).thenReturn(Optional.of(vinculoMock));
        when(vinculoMotoristaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        driverLinkingService.atualizarVerificacao(1L, 10L, "VERIFICADO", 1L);
        verify(vinculoMotoristaRepository).findByIdPrestadorIdAndIdMotoristaId(1L, 10L);
        assertEquals(1L, vinculoMock.getVerificadoPorId());
        assertEquals("VERIFICADO", vinculoMock.getStatusVerificacao());
    }
    private com.muv.backend.dto.MotoristaCadastroRequest cadastro() {
        return new com.muv.backend.dto.MotoristaCadastroRequest("Carlos Silva", "85999999999", "12345678900", "D",
                "12345678909", java.time.LocalDate.of(1990, 1, 1), "masculino", "motorista@example.test",
                "SenhaTeste!123", "SenhaTeste!123", null);
    }
}