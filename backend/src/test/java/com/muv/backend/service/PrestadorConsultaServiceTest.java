package com.muv.backend.service;

import com.muv.backend.repository.PrestadorConsultaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PrestadorConsultaServiceTest {
    @Mock PrestadorConsultaRepository repository;
    @InjectMocks PrestadorConsultaService service;

    @Test
    void normalizesOptionalFiltersBeforeSearching() {
        service.buscar(" pessoa ", " Fortaleza ", "ce", "autonomo");

        verify(repository).buscar("PESSOA", "Fortaleza", "CE", "AUTONOMO");
    }

    @Test
    void rejectsUnsupportedFiltersAndMalformedState() {
        assertThatThrownBy(() -> service.buscar("AMBOS", null, null, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.buscar(null, null, "Ceará", null))
                .isInstanceOf(ResponseStatusException.class);
    }
}
