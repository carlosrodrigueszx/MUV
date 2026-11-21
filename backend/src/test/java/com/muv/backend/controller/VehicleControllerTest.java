package com.muv.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.muv.backend.dto.VehicleResponse;
import com.muv.backend.service.VehicleService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VehicleControllerTest {
    private final VehicleService service = mock(VehicleService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new VehicleController(service)).build();
    }

    @Test
    void listsVehiclesForAuthenticatedEmail() throws Exception {
        when(service.list("provider@example.test")).thenReturn(List.of(vehicle()));

        mvc.perform(get("/api/veiculos").principal(() -> "provider@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].plate").value("ABC1D23"))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
        verify(service).list("provider@example.test");
    }

    @Test
    void rejectsInvalidVehicleData() throws Exception {
        mvc.perform(post("/api/veiculos").principal(() -> "provider@example.test")
                        .contentType("application/json")
                        .content("""
                                {"brand":"","model":"","year":1800,"plate":"??","color":"",
                                 "type":"invalido","capacity":0,"description":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsVehicle() throws Exception {
        when(service.create(org.mockito.ArgumentMatchers.eq("provider@example.test"),
                org.mockito.ArgumentMatchers.any())).thenReturn(vehicle());

        mvc.perform(post("/api/veiculos").principal(() -> "provider@example.test")
                        .contentType("application/json")
                        .content("""
                                {"brand":"Marca","model":"Modelo","year":2024,"plate":"ABC1D23",
                                 "color":"Branco","type":"passageiros","capacity":16,"description":"Van"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("passageiros"));
    }

    @Test
    void updatesVehicle() throws Exception {
        when(service.update(org.mockito.ArgumentMatchers.eq("provider@example.test"),
                org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.any())).thenReturn(vehicle());

        mvc.perform(put("/api/veiculos/42").principal(() -> "provider@example.test")
                        .contentType("application/json")
                        .content("""
                                {"brand":"Marca","model":"Modelo","year":2024,"plate":"ABC1D23",
                                 "color":"Branco","type":"passageiros","capacity":16,"description":"Van"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    private static VehicleResponse vehicle() {
        return new VehicleResponse(42L, "passageiros", "Marca", "Modelo", 2024,
                "ABC1D23", "Branco", BigDecimal.valueOf(16), "Van", "ativo");
    }
}
