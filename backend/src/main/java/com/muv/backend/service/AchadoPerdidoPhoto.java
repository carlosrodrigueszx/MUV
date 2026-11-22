package com.muv.backend.service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AchadoPerdidoPhoto {
    private static final int MAX_BYTES = 1_048_576;
    private static final int MAX_DIMENSION = 4096;

    record Content(byte[] bytes, String mimeType) {
        String dataUri() {
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }

    private AchadoPerdidoPhoto() {}

    static Content parse(String dataUri) {
        if (dataUri == null) throw invalid("Envie uma foto do objeto.");
        try {
            String[] parts = dataUri.split(",", 2);
            if (parts.length != 2 || !parts[0].matches("data:image/(png|jpeg|gif);base64"))
                throw invalid("Use uma foto PNG, JPEG ou GIF.");
            byte[] bytes = Base64.getDecoder().decode(parts[1]);
            if (bytes.length == 0 || bytes.length > MAX_BYTES)
                throw invalid("A foto deve ter entre 1 byte e 1 MB.");
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw invalid("Arquivo de foto inválido.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if (reader.getWidth(0) > MAX_DIMENSION || reader.getHeight(0) > MAX_DIMENSION)
                        throw invalid("A foto deve ter até 4096 pixels por dimensão.");
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!parts[0].equals("data:image/" + format + ";base64"))
                        throw invalid("O formato da foto não corresponde ao arquivo.");
                    return new Content(bytes, parts[0].substring(5, parts[0].indexOf(';')));
                } finally {
                    reader.dispose();
                }
            }
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw invalid("Arquivo de foto inválido.");
        }
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
