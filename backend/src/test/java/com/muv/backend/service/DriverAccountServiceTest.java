package com.muv.backend.service;

import com.muv.backend.dto.MotoristaCadastroRequest;
import com.muv.backend.model.Motorista;
import com.muv.backend.repository.*;
import com.muv.backend.security.DriverPersonalData;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverAccountServiceTest {
    @Mock AuthRepository accounts;
    @Mock MotoristaRepository drivers;
    @Mock UsuarioRepository users;
    @Mock PasswordEncoder passwords;
    @Mock DriverPersonalData privacy;
    @InjectMocks DriverAccountService service;
    MotoristaCadastroRequest valid(String confirmation, String cpf, String photo) {
        return new MotoristaCadastroRequest("Motorista Teste", "(85) 90000-0000", "12345678900", "D", cpf,
                LocalDate.of(1990, 1, 1), "masculino", "MOTORISTA@example.test", "SenhaTeste!123", confirmation, photo);
    }
    @Test void criaContaComHashELigaMotoristaSemCriarPassageiro() {
        when(privacy.cpfIndex("12345678909")).thenReturn("index");
        when(passwords.encode("SenhaTeste!123")).thenReturn("hash");
        when(accounts.insertAccount("Motorista Teste", "85900000000", "motorista@example.test", "hash")).thenReturn(42L);
        when(privacy.encrypt("12345678909\n1990-01-01\nmasculino", 42L)).thenReturn("encrypted");
        when(drivers.save(any())).thenAnswer(i -> i.getArgument(0));
        var driver = service.cadastrar(valid("SenhaTeste!123", "123.456.789-09", null));
        assertEquals(42L, driver.getUsuarioId());
        assertEquals("encrypted", driver.getDadosCifrados());
        assertEquals("index", driver.getCpfIndice());
        verify(accounts, never()).insertPassenger(anyLong(), any(), any());
    }
    @Test void rejeitaSenhasDiferentesAntesDePersistir() {
        assertThrows(ResponseStatusException.class, () -> service.cadastrar(valid("OutraSenha!", "12345678909", null)));
        verifyNoInteractions(accounts, drivers, passwords);
    }
    @Test void rejeitaCpfInvalidoAntesDePersistir() {
        assertThrows(ResponseStatusException.class, () -> service.cadastrar(valid("SenhaTeste!123", "11111111111", null)));
        verifyNoInteractions(accounts, drivers, passwords);
    }
    @Test void rejeitaEmailExistenteSemTrocarSenhaDaConta() {
        when(privacy.cpfIndex("12345678909")).thenReturn("index");
        when(accounts.findAccount("motorista@example.test")).thenReturn(Optional.of(new AuthRepository.Account(1, "Existente", "motorista@example.test", "hash")));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.cadastrar(valid("SenhaTeste!123", "12345678909", null))).getStatusCode().value());
        verify(accounts, never()).insertAccount(any(), any(), any(), any());
        verifyNoInteractions(passwords);
    }
    @Test void rejeitaArquivoQueNaoSejaImagemAntesDeCriarConta() {
        when(privacy.cpfIndex("12345678909")).thenReturn("index");
        assertThrows(ResponseStatusException.class, () -> service.cadastrar(valid("SenhaTeste!123", "12345678909", "data:image/png;base64,ZXJybw==")));
        verify(accounts, never()).insertAccount(any(), any(), any(), any());
        verify(drivers, never()).save(any());
    }
}
