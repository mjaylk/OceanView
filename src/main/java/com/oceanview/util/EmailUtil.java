package com.oceanview.util;

import com.oceanview.service.SettingService;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {

    private static final SettingService settingService = new SettingService();

    private static boolean isTrue(String s, boolean def) {
        if (s == null) return def;
        String v = s.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
    }

    public static void send(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.trim().isEmpty()) return;

    
        String host = settingService.getValue("smtp_host");
        String port = settingService.getValueOrDefault("smtp_port", "587");
        String user = settingService.getValue("smtp_username");
        String pass = settingService.getValue("smtp_password");

        boolean auth = isTrue(settingService.getValue("smtp_use_auth"), true);
        boolean tls = isTrue(settingService.getValue("smtp_use_tls"), true);

        String fromEmail = settingService.getValueOrDefault("smtp_from_email", user);
        String fromName = settingService.getValueOrDefault("smtp_from_name", "OceanView Resort");

        System.out.println("SMTP host=" + host + ", port=" + port + ", user=" + user + ", auth=" + auth + ", tls=" + tls);

        if (host == null || host.trim().isEmpty())
            throw new RuntimeException("SMTP host is missing (smtp_host)");
        if (user == null || user.trim().isEmpty())
            throw new RuntimeException("SMTP username is missing (smtp_username)");
        if (pass == null || pass.trim().isEmpty())
            throw new RuntimeException("SMTP password is missing (smtp_password)");

        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(tls));
        props.put("mail.smtp.host", host.trim());
        props.put("mail.smtp.port", port.trim());

 
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");


        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user.trim(), pass.trim());
            }
        });

        try {
            Message message = new MimeMessage(session);

       
            message.setFrom(new InternetAddress(fromEmail.trim(), fromName));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail.trim()));
            message.setSubject(subject == null ? "" : subject);
            message.setText(body == null ? "" : body);

            Transport.send(message);
            System.out.println("Email sent to: " + toEmail);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }
}
