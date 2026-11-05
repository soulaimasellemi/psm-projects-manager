package com.smartsoft.psm.service;

import com.smartsoft.psm.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** Envoi des emails HTML (asynchrone : une panne SMTP ne bloque jamais l'API). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${psm.admin.email}")
    private String adminEmail;

    @Value("${psm.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Notifie l'admin qu'un nouvel utilisateur s'est inscrit. */
    @Async
    public void notifierAdminInscription(User u) {
        String contenu = """
                <p>Bonjour,</p>
                <p>Un nouvel utilisateur vient de s'inscrire sur <b>Projects Manager</b> et attend votre validation :</p>
                <table style="border-collapse:collapse;margin:16px 0">
                  <tr><td style="padding:6px 12px;color:#8898aa">Nom</td><td style="padding:6px 12px"><b>%s %s</b></td></tr>
                  <tr><td style="padding:6px 12px;color:#8898aa">Email</td><td style="padding:6px 12px">%s</td></tr>
                  <tr><td style="padding:6px 12px;color:#8898aa">Téléphone</td><td style="padding:6px 12px">%s</td></tr>
                </table>
                <p><a href="%s/utilisateurs" style="background:#5e72e4;color:#fff;padding:12px 22px;border-radius:6px;text-decoration:none;font-weight:600">Valider le compte</a></p>
                """.formatted(esc(u.getPrenom()), esc(u.getNom()), esc(u.getEmail()),
                u.getTel() == null || u.getTel().isBlank() ? "-" : esc(u.getTel()), frontendUrl);
        envoyer(adminEmail, "[PSM] Nouvelle inscription : " + u.getPrenom() + " " + u.getNom(), contenu);
    }

    /** Prévient l'utilisateur que son compte a été activé. */
    @Async
    public void notifierActivation(User u) {
        String contenu = """
                <p>Bonjour <b>%s</b>,</p>
                <p>Bonne nouvelle : votre compte <b>Projects Manager</b> vient d'être validé par l'administrateur.</p>
                <p><a href="%s/login" style="background:#2dce89;color:#fff;padding:12px 22px;border-radius:6px;text-decoration:none;font-weight:600">Se connecter</a></p>
                """.formatted(esc(u.getPrenom()), frontendUrl);
        envoyer(u.getEmail(), "[PSM] Votre compte est activé", contenu);
    }

    private void envoyer(String to, String sujet, String contenu) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, "SmartSoft - Projects Manager");
            helper.setTo(to);
            helper.setSubject(sujet);
            helper.setText(gabarit(contenu), true);
            mailSender.send(message);
            log.info("Email envoyé à {} : {}", to, sujet);
        } catch (Exception e) {
            log.warn("Échec d'envoi de l'email à {} ({}). Vérifiez la config spring.mail.*", to, e.getMessage());
        }
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }

    private static String gabarit(String contenu) {
        return """
                <div style="font-family:'Open Sans',Arial,sans-serif;background:#f8f9fe;padding:32px">
                  <div style="max-width:560px;margin:auto;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 0 2rem rgba(136,152,170,.15)">
                    <div style="background:linear-gradient(87deg,#5e72e4,#825ee4);padding:24px;color:#fff">
                      <h2 style="margin:0">SmartSoft Solutions</h2>
                      <div style="opacity:.85">Projects Manager</div>
                    </div>
                    <div style="padding:24px;color:#525f7f;font-size:15px;line-height:1.6">%s</div>
                    <div style="padding:16px 24px;background:#f6f9fc;color:#8898aa;font-size:12px">Email automatique, merci de ne pas répondre.</div>
                  </div>
                </div>
                """.formatted(contenu);
    }
}
