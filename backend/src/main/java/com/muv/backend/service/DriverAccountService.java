package com.muv.backend.service;

import com.muv.backend.dto.MotoristaCadastroRequest;
import com.muv.backend.dto.MotoristaConsultaResponse;
import com.muv.backend.model.*;
import com.muv.backend.repository.*;
import com.muv.backend.security.DriverPersonalData;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DriverAccountService {
    private final AuthRepository accounts;
    private final MotoristaRepository drivers;
    private final UsuarioRepository users;
    private final PasswordEncoder passwords;
    private final DriverPersonalData privacy;

    public DriverAccountService(AuthRepository accounts, MotoristaRepository drivers, UsuarioRepository users,
            PasswordEncoder passwords, @Lazy DriverPersonalData privacy) {
        this.accounts = accounts; this.drivers = drivers; this.users = users;
        this.passwords = passwords; this.privacy = privacy;
    }

    @Transactional
    public Motorista cadastrar(MotoristaCadastroRequest data) {
        if (!data.senha().equals(data.confirmarSenha())) throw invalid("As senhas não coincidem.");
        if (data.senha().getBytes(StandardCharsets.UTF_8).length > 72) throw invalid("A senha excede o limite de 72 bytes.");
        String cpf = data.cpf().replaceAll("[^0-9]", "");
        if (!AuthService.isValidCpf(cpf)) throw invalid("Informe um CPF válido.");
        String phone = data.telefone().replaceAll("[^0-9]", "");
        if (!phone.matches("(?:55)?[0-9]{10,11}")) throw invalid("Informe um telefone com DDD.");
        String email = AuthService.normalizeEmail(data.email());
        String index = privacy.cpfIndex(cpf);
        if (accounts.findAccount(email).isPresent() || drivers.existsByCpfIndice(index))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail ou CPF.");
        Motorista driver = new Motorista();
        setPhoto(driver, data.foto());
        long accountId = accounts.insertAccount(data.nome().trim(), phone, email, passwords.encode(data.senha()));
        driver.setNome(data.nome().trim()); driver.setTelefone(phone); driver.setCnh(data.cnh());
        driver.setCategoriaCnh(data.categoriaCnh()); driver.setUsuarioId(accountId); driver.setCpfIndice(index);
        driver.setDadosCifrados(privacy.encrypt(cpf + "\n" + data.nascimento() + "\n" + data.genero().trim(), accountId));
        return drivers.save(driver);
    }

    public Motorista buscar(String identifier) {
        String value = identifier.trim();
        if (value.contains("@")) return drivers.findByAccountEmail(AuthService.normalizeEmail(value))
                .orElseThrow(DriverAccountService::notFound);
        String cpf = value.replaceAll("[^0-9]", "");
        if (!cpf.matches("[0-9]{11}")) throw invalid("Informe um CPF ou e-mail válido.");
        return drivers.findByCpfIndice(privacy.cpfIndex(cpf)).orElseThrow(DriverAccountService::notFound);
    }

    public MotoristaConsultaResponse resposta(Motorista m) { return resposta(m, null, null); }
    public MotoristaConsultaResponse resposta(VinculoMotorista v) {
        return resposta(v.getMotorista(), v.getStatusVerificacao(), v.getDataVerificacao());
    }
    private MotoristaConsultaResponse resposta(Motorista m, String status, java.time.LocalDateTime date) {
        String cpf = null, birth = null, gender = null, email = null;
        if (m.getDadosCifrados() != null) {
            String[] data = privacy.decrypt(m.getDadosCifrados(), m.getUsuarioId()).split("\n", 3);
            cpf = data[0]; birth = data[1]; gender = data[2];
        }
        if (m.getUsuarioId() != null) email = users.findById(m.getUsuarioId())
                .orElseThrow(() -> new IllegalStateException("Conta do motorista não encontrada.")).getEmail();
        String avatar = m.getFoto() == null ? null : "data:" + m.getFotoTipo() + ";base64," + Base64.getEncoder().encodeToString(m.getFoto());
        return new MotoristaConsultaResponse(m.getId(), m.getNome(), m.getTelefone(), m.getCnh(),
                m.getCategoriaCnh(), status, date, cpf, birth, gender, email, avatar);
    }

    private static void setPhoto(Motorista driver, String photo) {
        if (photo == null || photo.isBlank()) return;
        try {
            String[] parts = photo.split(",", 2);
            if (parts.length != 2 || !parts[0].matches("data:image/(png|jpeg|gif);base64")) throw invalid("Use uma foto PNG, JPEG ou GIF.");
            byte[] bytes = Base64.getDecoder().decode(parts[1]);
            if (bytes.length > 1048576) throw invalid("A foto deve ter no máximo 1 MB.");
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid("Arquivo de foto inválido.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if (reader.getWidth(0) > 4096 || reader.getHeight(0) > 4096) throw invalid("A foto deve ter até 4096 pixels por dimensão.");
                    String type = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!parts[0].equals("data:image/" + type + ";base64")) throw invalid("O formato da foto não corresponde ao arquivo.");
                } finally { reader.dispose(); }
            }
            driver.setFoto(bytes); driver.setFotoTipo(parts[0].substring(5, parts[0].indexOf(';')));
        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) { throw invalid("Arquivo de foto inválido."); }
    }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado."); }
}
