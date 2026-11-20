package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.muv.backend.dto.VehicleRequest;
import com.muv.backend.dto.VehicleResponse;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.repository.PrestadorRepository;
import com.muv.backend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class VehicleServiceTest {
    private final VehicleRepository vehicles = mock(VehicleRepository.class);
    private final AuthRepository accounts = mock(AuthRepository.class);
    private final PrestadorRepository providers = mock(PrestadorRepository.class);
    private VehicleService service;

    @BeforeEach
    void setUp() {
        service = new VehicleService(vehicles, accounts, providers);
        when(accounts.findAccount("prestador@example.test"))
                .thenReturn(Optional.of(new AuthRepository.Account(21L, "Prestador", "prestador@example.test", "hash")));
        when(providers.existsById(21L)).thenReturn(true);
    }

    @Test
    void listsOnlyVehiclesForAuthenticatedProvider() {
        when(vehicles.findAllByProvider(21L)).thenReturn(List.of(vehicle(10L, "ABC1D23")));

        assertThat(service.list("Prestador@Example.Test")).hasSize(1);
        verify(vehicles).findAllByProvider(21L);
    }

    @Test
    void mapsPassengerCapacityAndNormalizesPlateOnCreate() {
        VehicleRequest request = request("passageiros", "abc-1d23", "16");
        when(vehicles.insert(eq(21L), eq(request), eq("ABC1D23"), eq("PESSOA"), eq(16),
                eq(BigDecimal.ZERO))).thenReturn(vehicle(10L, "ABC1D23"));

        VehicleResponse saved = service.create("prestador@example.test", request);

        assertThat(saved.id()).isEqualTo(10L);
        assertThat(saved.plate()).isEqualTo("ABC1D23");
        verify(vehicles).insert(eq(21L), eq(request), eq("ABC1D23"), eq("PESSOA"), eq(16),
                eq(BigDecimal.ZERO));
    }

    @Test
    void rejectsFractionalPassengerCapacity() {
        assertThatThrownBy(() -> service.create("prestador@example.test",
                request("passageiros", "ABC1D23", "16.5")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("número inteiro");
    }

    @Test
    void rejectsAccountThatIsNotAProvider() {
        when(providers.existsById(21L)).thenReturn(false);

        assertThatThrownBy(() -> service.list("prestador@example.test"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exclusiva de prestadores");
    }

    @Test
    void returnsNotFoundWhenVehicleDoesNotBelongToProvider() {
        when(vehicles.findByIdAndProvider(99L, 21L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("prestador@example.test", 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Veículo não encontrado");
    }

    private static VehicleRequest request(String type, String plate, String capacity) {
        return new VehicleRequest("Marca", "Modelo", 2024, plate, "Branco", type,
                new BigDecimal(capacity), "Descrição");
    }

    private static VehicleResponse vehicle(long id, String plate) {
        return new VehicleResponse(id, "passageiros", "Marca", "Modelo", 2024, plate,
                "Branco", BigDecimal.valueOf(16), "Descrição", "ativo");
    }
}
