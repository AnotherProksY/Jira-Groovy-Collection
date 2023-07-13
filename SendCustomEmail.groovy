/* Пакет с для отправки писем в JIRA. */

package com.custom

import java.util.*;                              // Утилиты
import javax.mail.*;                             // Почта
import javax.mail.internet.*;                    //
import javax.activation.*;                       // MIME
import org.apache.log4j.Logger;                  // Логгер

class SendCustomEmail {

    /* Интерфейсы */
	private static final Logger logger = Logger.getLogger("groovy.errorlog.class");

    /* Статика */
    private static final String host = "<smtp.domain.com>";
    private static final String username = "<username>";
    private static final String password = "<password>";


    // ------------------- "ОТПРАВКА ПОЧТЫ" ---------------------
	public static void send(String subject, String body, String from, String to) {

        try {
            // Получаем адреса в нужном формате
            List<String> mail = parseAddress(to);

            // Настраиваем и получаем готовый объект письма
            MimeMessage message = setMailProperties(subject, body, from, mail);
            log("Setting system mail properties: "+from+", "+to);

            // Отправляем!
            Transport.send(message);
            log("Mail was send from: "+from+", to: "+to);
        } catch (Exception error){
            log(error);
        }
    }

    private static List<String> parseAddress(String address) {

        List<String> domains = [];

        if (!address.contains(',')) {
            domains.add(address);
        } else { 
            domains = address.split(',');
        }

        return domains;
    }

    private static MimeMessage setMailProperties(String subject, String body, String from, List<String> to) {

        // Получаем объект настроек
        Properties properties = System.getProperties();

        // Указываем параметры почтового сервера
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.port", "25");

        // Получаем объект сессии
        Session session = Session.getInstance(properties,
        new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Создаем объект письма
        MimeMessage message = new MimeMessage(session);

        // От кого отправляем письмо
        message.setFrom(new InternetAddress(from));

        // Кому отправляем письмо
        for (mail in to) {
            message.addRecipient(Message.RecipientType.TO,new InternetAddress(mail));
        }

        // Добавляем тему письма
        message.setSubject(subject);

        // Добавляем контент письма
        message.setContent(body, "text/html; charset=utf-8");

        return message;
    }
    //


    // ------------------- "ЛОГИРОВАНИЕ" ------------------------
    private static void log(Exception message) {
        logger.debug("[${this.getSimpleName()}] " + message);
        logger.debug("[${this.getSimpleName()}] " + message.getMessage().toString());
        logger.debug("[${this.getSimpleName()}] " + message.getStackTrace().toString().replace(',',',\n').replace('[','').replace(']',''));
    }
    private static void log(String message) {
        logger.warn("[${this.getSimpleName()}] " + message);
    }
    //
}
