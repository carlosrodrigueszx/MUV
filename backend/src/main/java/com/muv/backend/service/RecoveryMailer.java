package com.muv.backend.service;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecoveryMailer {
    private static final Logger log = LoggerFactory.getLogger(RecoveryMailer.class);
    private final ObjectProvider<JavaMailSender> mail;
    private final String from;
    private final URI publicOrigin;

    public RecoveryMailer(ObjectProvider<JavaMailSender> mail,
                          @Value("${muv.auth.mail-from:}") String from,
                          @Value("${muv.auth.public-origin:http://localhost:5173}") URI publicOrigin) {
        this.mail = mail;
        this.from = from;
        this.publicOrigin = publicOrigin;
    }

    @Async
    @TransactionalEventListener
    public void send(AuthService.RecoveryMail event) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null || from.isBlank()) {
            log.warn("Envio de recuperação indisponível: configure SMTP e o remetente.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject("Redefinição de senha do MUV");
        message.setText("Redefina sua senha em até 30 minutos:\n"
                + publicOrigin.resolve("/redefinir-senha#token=" + event.token())
                + "\n\nSe você não fez essa solicitação, ignore esta mensagem.");
        try {
            sender.send(message);
        } catch (MailException e) {
            log.warn("O SMTP não conseguiu enviar uma mensagem de recuperação.");
        }
    }
}
