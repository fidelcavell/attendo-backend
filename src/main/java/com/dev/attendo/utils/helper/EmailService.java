package com.dev.attendo.utils.helper;

import com.dev.attendo.exception.InternalServerErrorException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String resetUrl, String title, String username, String content, String button, String expiredTime) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            String htmlContent = """
                        <html>
                          <body style="font-family: Arial, sans-serif; background-color: #f6f9fc; padding: 20px;">
                            <div style="max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; padding: 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                              <h2 style="color: #2c3e50;">%s</h2>
                              <p>Hello, <b>%s</b>,</p>
                              <p>%s</p>
                              <p style="text-align: center; margin: 30px 0;">
                                <a href="%s" style="background-color: #007bff; color: white; text-decoration: none; padding: 12px 25px; border-radius: 5px; display: inline-block;">
                                  %s
                                </a>
                              </p>
                              <p>If you didn’t request this, please ignore this email.</p>
                              <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                              <p style="font-size: 12px; color: #999;">This link will expire in %s.</p>
                              <div style="text-align: right; margin-top: 30px;">
                                <p style="font-size: 11px; color: #aaa;">© 2025 Attendo. All rights reserved.</p>
                              </div>
                            </div>
                          </body>
                        </html>
                    """.formatted(title, username, content, resetUrl, button, expiredTime);

            helper.setTo(to);
            helper.setSubject(title);
            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new InternalServerErrorException("Failed to send email: " + e.getMessage());
        }
    }
}
