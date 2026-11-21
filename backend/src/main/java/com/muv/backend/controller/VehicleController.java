package com.muv.backend.controller;

import com.muv.backend.dto.VehicleRequest;
import com.muv.backend.dto.VehicleResponse;
import com.muv.backend.service.VehicleService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/veiculos")
@CrossOrigin(origins = "${muv.frontend-origin:http://localhost:5173}", allowCredentials = "true")
public class VehicleController {
    private final VehicleService vehicles;

    public VehicleController(VehicleService vehicles) {
        this.vehicles = vehicles;
    }

    @GetMapping
    public List<VehicleResponse> list(Principal principal) {
        return vehicles.list(principal.getName());
    }

    @GetMapping("/{id}")
    public VehicleResponse get(@PathVariable long id, Principal principal) {
        return vehicles.get(principal.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request, Principal principal) {
        return vehicles.create(principal.getName(), request);
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable long id, @Valid @RequestBody VehicleRequest request,
            Principal principal) {
        return vehicles.update(principal.getName(), id, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidFields() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Confira os campos informados para o veículo.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejected(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }
}
