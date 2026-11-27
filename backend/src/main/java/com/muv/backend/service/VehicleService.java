package com.muv.backend.service;

import com.muv.backend.dto.VehicleRequest;
import com.muv.backend.dto.VehicleResponse;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.repository.PrestadorRepository;
import com.muv.backend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicles;
    private final AuthRepository accounts;
    private final PrestadorRepository providers;

    public VehicleService(VehicleRepository vehicles, AuthRepository accounts, PrestadorRepository providers) {
        this.vehicles = vehicles;
        this.accounts = accounts;
        this.providers = providers;
    }

    public List<VehicleResponse> list(String email) {
        return vehicles.findAllByProvider(providerId(email));
    }

    public VehicleResponse get(String email, long id) {
        return vehicles.findByIdAndProvider(id, providerId(email))
                .orElseThrow(() -> notFound());
    }

    @Transactional
    public VehicleResponse create(String email, VehicleRequest request) {
        long providerId = providerId(email);
        VehicleValues values = values(request);
        try {
            return vehicles.insert(providerId, request, values.plate(), values.modality(),
                    values.passengerCapacity(), values.cargoCapacity());
        } catch (DataIntegrityViolationException exception) {
            throw duplicatePlate(exception);
        }
    }

    @Transactional
    public VehicleResponse update(String email, long id, VehicleRequest request) {
        long providerId = providerId(email);
        VehicleValues values = values(request);
        String currentModality = vehicles.findModalityByIdAndProvider(id, providerId)
                .orElseThrow(VehicleService::notFound);
        if ("AMBOS".equals(currentModality) && "carga".equals(request.type())) {
            values = new VehicleValues(values.plate(), "AMBOS", 0, values.cargoCapacity());
        }
        try {
            if (!vehicles.update(id, providerId, request, values.plate(), values.modality(),
                    values.passengerCapacity(), values.cargoCapacity())) {
                throw notFound();
            }
            return get(email, id);
        } catch (DataIntegrityViolationException exception) {
            throw duplicatePlate(exception);
        }
    }

    private long providerId(String email) {
        long id = accounts.findAccount(AuthService.normalizeEmail(email))
                .map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "A conta autenticada não foi encontrada."));
        if (!providers.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A gestão de veículos é exclusiva de prestadores.");
        }
        return id;
    }

    private static VehicleValues values(VehicleRequest request) {
        String plate = request.plate().replace("-", "").trim().toUpperCase(java.util.Locale.ROOT);
        if ("passageiros".equals(request.type())) {
            try {
                return new VehicleValues(plate, "PESSOA", request.capacity().intValueExact(), BigDecimal.ZERO);
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A capacidade de passageiros deve ser um número inteiro.");
            }
        }
        return new VehicleValues(plate, "ENCOMENDA", 0, request.capacity());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado.");
    }

    private static ResponseStatusException duplicatePlate(Exception cause) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Já existe um veículo cadastrado com essa placa.", cause);
    }

    private record VehicleValues(String plate, String modality, int passengerCapacity,
            BigDecimal cargoCapacity) {}
}
