package com.oceanview.util;

import com.oceanview.service.SettingService;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {

    // service layer usage
    private static final SettingService settingService = new SettingService();

    // helper method
    private static boolean isTrue(String s, boolean def) {
        if (s == null) return def;
        String v = s.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
    }

    // utility method
    public static void send(String toEmail, String subject, String body) {

        // input validation
        if (toEmail == null || toEmail.trim().isEmpty()) return;

        // config from database
        String host = settingService.getValue("smtp_host");
        String port = settingService.getValueOrDefault("smtp_port", "587");
        String user = settingService.getValue("smtp_username");
        String pass = settingService.getValue("smtp_password");

        boolean auth = isTrue(settingService.getValue("smtp_use_auth"), true);
        boolean tls = isTrue(settingService.getValue("smtp_use_tls"), true);

        String fromEmail = settingService.getValueOrDefault("smtp_from_email", user);
        String fromName = settingService.getValueOrDefault("smtp_from_name", "OceanView Resort");

        // debug logging
        System.out.println("SMTP host=" + host + ", port=" + port + ", user=" + user);

        // error handling
        if (host == null || host.trim().isEmpty())
            throw new RuntimeException("SMTP host missing");
        if (user == null || user.trim().isEmpty())
            throw new RuntimeException("SMTP username missing");
        if (pass == null || pass.trim().isEmpty())
            throw new RuntimeException("SMTP password missing");

        // mail properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(tls));
        props.put("mail.smtp.host", host.trim());
        props.put("mail.smtp.port", port.trim());

        // timeout config
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        // authentication
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user.trim(), pass.trim());
            }
        });

        try {
            Message message = new MimeMessage(session);

            // message setup
            message.setFrom(new InternetAddress(fromEmail.trim(), fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail.trim()));
            message.setSubject(subject == null ? "" : subject);
            message.setText(body == null ? "" : body);

            Transport.send(message);
            System.out.println("Email sent: SUCCESS");

        } catch (Exception e) {
            System.out.println("Email sent: FAILED");
            e.printStackTrace();
        }
    }

    // test cases
    public static void main(String[] args) {

        // test case 01 null email
        try {
            send(null, "Test", "Test body");
            System.out.println("Test 01 Null email: PASSED");
        } catch (Exception e) {
            System.out.println("Test 01 Null email: FAILED");
        }

        // test case 02 empty email
        try {
            send("", "Test", "Test body");
            System.out.println("Test 02 Empty email: PASSED");
        } catch (Exception e) {
            System.out.println("Test 02 Empty email: FAILED");
        }

        // test case 03 real email test
        try {
            send("test@example.com", "Test Email", "This is test email");
            System.out.println("Test 03 Send email: CHECK CONSOLE");
        } catch (Exception e) {
            System.out.println("Test 03 Send email: FAILED");
        }
    }
}
