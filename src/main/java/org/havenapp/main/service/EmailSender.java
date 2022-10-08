package org.havenapp.main.service;

import android.util.Log;

import org.havenapp.main.PreferenceManager;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

// TODO add heartBeat via email
class EmailSender {
    private static EmailSender INSTANCE;

    private final PreferenceManager prefs;
    private final Session session;

    private EmailSender(PreferenceManager prefs) {
        this.prefs = prefs;
        Properties props = new Properties();
        props.put("mail.smtp.host", prefs.getEmailServer());
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.auth", "true");
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.port", "465");
        //props.put("mail.debug", "true");
        //props.put("mail.verbose", "true");

        session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(prefs.getEmailAccount(), prefs.getEmailPassword());
            }
        });
        //session.setDebug(true);
    }

    static synchronized EmailSender getInstance(PreferenceManager mPrefs) {
        if (INSTANCE == null) {
            INSTANCE = new EmailSender( mPrefs);
        }
        return INSTANCE;
    }

    void send(String message) {
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(prefs.getEmailSender()));
            msg.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(prefs.getEmailRecipient()));
            msg.setSubject("Haven");
            msg.setText(message);
            msg.setSentDate(new Date());

            new Thread() {
                @Override
                public void run() {
                    try {
                        Transport.send(msg);
                        Log.i("EmailSender", "email sent");
                    } catch(Exception e) {
                        Log.e("EmailSender", "error occurred", e);
                    }
                }
            }.start();

            Log.i("EmailSender", "email sending started");
        } catch(Exception e) {
            Log.e("EmailSender", "Stopped", e);
        }
    }

    void sendWithAttachment(String message, String attachmentPath) {
        try {
            Multipart mp = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setText(message);
            mp.addBodyPart(htmlPart);

            Log.i("EmailSender", "attachmentPath: " + attachmentPath);

            if (attachmentPath != null) {
                addAttachment(mp, attachmentPath);
            }

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(prefs.getEmailSender()));
            msg.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(prefs.getEmailRecipient()));
            msg.setSubject("Haven Alert");
            msg.setContent(mp);

            new Thread() {
                @Override
                public void run() {
                    try {
                        Transport.send(msg);
                        Log.i("EmailSender", "email sent");
                    } catch(Exception e) {
                        Log.e("EmailSender", "error occurred", e);
                    }
                }
            }.start();

            Log.i("EmailSender", "email sending started");
        } catch(Exception e) {
            Log.e("EmailSender", "Stopped", e);
        }
    }

    private void addAttachment(Multipart msg, String filename) throws Exception {
        BodyPart messageBodyPart = new MimeBodyPart();
        File fileMedia = new File(filename);
        DataSource source = new FileDataSource(fileMedia);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        msg.addBodyPart(messageBodyPart);
    }
}
