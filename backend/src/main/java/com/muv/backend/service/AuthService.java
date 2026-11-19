package com.muv.backend.service;

import com.muv.backend.dto.AuthRequests.CompanyRegistration;
import com.muv.backend.dto.AuthRequests.IndependentRegistration;
import com.muv.backend.dto.AuthRequests.Registration;
import com.muv.backend.dto.AuthRequests.Reset;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.security.PassengerPrivacy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AuthRepository accounts;
    private final PassengerPrivacy privacy;
    private final PasswordEncoder passwords;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AuthRepository accounts, @Lazy PassengerPrivacy privacy, PasswordEncoder passwords,
                       ApplicationEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.privacy = privacy;
        this.passwords = passwords;
        this.events = events;
        this.clock = clock;
    }

    public record RecoveryMail(String email, String token) {
        @Override public String toString() { return "RecoveryMail[redacted]"; }
    }

    @Transactional
    public void register(Registration request) {
        insertPassengerAccount(request);
    }

    @Transactional
    public void registerCompany(CompanyRegistration request) {
        checkPassword(request.senha(), request.confirmarSenha());
        String cnpj = request.cnpj().replaceAll("[^0-9]", "");
        if (!isValidCnpj(cnpj)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um CNPJ válido.");
        }
        String email = normalizeEmail(request.email());
        if (accounts.findAccount(email).isPresent() || accounts.hasCompanyDocument(cnpj)) {
            throw duplicateCompany();
        }

        try {
            long id = accounts.insertAccount(request.razaoSocial().trim(), normalizePhone(request.telefone()),
                    email, passwords.encode(request.senha()));
            accounts.recordTermsAcceptance(id);
            accounts.insertProvider(id);
            accounts.insertCompany(id, cnpj, request.razaoSocial().trim(), request.nomeFantasia().trim());
        } catch (DuplicateKeyException exception) {
            throw duplicateCompany();
        }
    }

    @Transactional
    public void registerIndependent(IndependentRegistration request) {
        long id = insertPassengerAccount(request.conta());
        try {
            accounts.insertProvider(id);
            accounts.insertIndependent(id, privacy.encrypt(request.cnh() + "\n" + request.categoriaCnh(), id));
        } catch (DuplicateKeyException exception) {
            throw duplicateRegistration();
        }
    }

    private long insertPassengerAccount(Registration request) {
        checkPassword(request.senha(), request.confirmarSenha());
        String cpf = request.cpf().replaceAll("[^0-9]", "");
        if (!isValidCpf(cpf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um CPF válido.");
        }
        String telefone = normalizePhone(request.telefone());
        String email = normalizeEmail(request.email());
        String index = privacy.cpfIndex(cpf);
        if (accounts.findAccount(email).isPresent() || accounts.hasCpf(cpf, index)) {
            throw duplicateRegistration();
        }

        try {
            long id = accounts.insertAccount(request.nome().trim(), telefone, email,
                    passwords.encode(request.senha()));
            accounts.recordTermsAcceptance(id);
            String personalData = cpf + "\n" + request.nascimento() + "\n" + request.genero().trim();
            accounts.insertPassenger(id, index, privacy.encrypt(personalData, id));
            return id;
        } catch (DuplicateKeyException exception) {
            throw duplicateRegistration();
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        accounts.findAccount(normalizeEmail(email)).ifPresent(account -> {
            accounts.lockAccount(account.id());
            LocalDateTime now = LocalDateTime.now(clock);
            accounts.invalidateResets(account.id(), now);
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            accounts.insertReset(account.id(), digest(token), now.plusMinutes(30));
            events.publishEvent(new RecoveryMail(account.email(), token));
        });
    }

    @Transactional
    public void resetPassword(Reset request) {
        checkPassword(request.senha(), request.confirmarSenha());
        String digest = digest(request.token());
        long id = accounts.resetOwner(digest).orElseThrow(AuthService::invalidReset);
        accounts.lockAccount(id);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!accounts.consumeReset(digest, now)) {
            throw invalidReset();
        }
        accounts.replacePassword(id, passwords.encode(request.senha()));
        accounts.invalidateResets(id, now);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (!digits.matches("(?:55)?[0-9]{10,11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um telefone com DDD.");
        }
        return digits;
    }

    private static boolean isValidCnpj(String cnpj) {
        if (!cnpj.matches("[0-9]{14}") || cnpj.chars().distinct().count() == 1) return false;
        for (int length = 12; length <= 13; length++) {
            int sum = 0;
            for (int i = 0; i < length; i++) {
                sum += (cnpj.charAt(i) - '0') * (2 + (length - 1 - i) % 8);
            }
            int remainder = sum % 11;
            int digit = remainder < 2 ? 0 : 11 - remainder;
            if (digit != cnpj.charAt(length) - '0') return false;
        }
        return true;
    }

    private static ResponseStatusException duplicateCompany() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail ou CNPJ.");
    }

    static boolean isValidCpf(String cpf) {
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int length = 9; length <= 10; length++) {
            int sum = 0;
            for (int i = 0; i < length; i++) sum += (cpf.charAt(i) - '0') * (length + 1 - i);
            if ((sum * 10 % 11) % 10 != cpf.charAt(length) - '0') return false;
        }
        return true;
    }

    private static String digest(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void checkPassword(String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As senhas não coincidem.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha excede o limite de 72 bytes.");
        }
    }

    private static ResponseStatusException duplicateRegistration() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail ou CPF.");
    }

    private static ResponseStatusException invalidReset() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado. Solicite outro.");
    }
}
