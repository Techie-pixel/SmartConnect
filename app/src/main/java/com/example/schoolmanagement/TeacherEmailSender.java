package com.example.schoolmanagement;

import android.os.AsyncTask;
import android.util.Log;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class TeacherEmailSender {

    private static final String TAG = "TeacherEmailSender";

    // Apna email aur 16-digit app password yahan daal
    private static final String SENDER_EMAIL = "schoolmanagement748@gmail.com";
    private static final String SENDER_PASSWORD = "wxxllzangcdxhese";

    public static void sendOrderConfirmation(String teacherEmail, String teacherName,
            String orderDetails, String totalAmount) {
        new SendTeacherEmailTask().execute(teacherEmail, teacherName, orderDetails, totalAmount);
    }

    private static class SendTeacherEmailTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String teacherEmail = params[0];
            String teacherName = params[1];
            String orderDetails = params[2];
            String totalAmount = params[3];

            try {
                // Gmail SMTP configuration
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                // Create session
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                // Create message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "Smartconnect store"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(teacherEmail));
                message.setSubject("✅ Order Confirmed - Payment Successful");

                // Beautiful HTML email body
                String emailBody = "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }" +
                        ".container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 600px; margin: 0 auto; }"
                        +
                        ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; text-align: center; }"
                        +
                        ".success-icon { font-size: 50px; margin-bottom: 10px; }" +
                        ".order-details { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #4CAF50; }"
                        +
                        ".item { padding: 10px 0; border-bottom: 1px solid #e0e0e0; }" +
                        ".total { background: #4CAF50; color: white; padding: 15px; border-radius: 8px; text-align: center; font-size: 24px; font-weight: bold; margin-top: 20px; }"
                        +
                        ".footer { text-align: center; color: #888; margin-top: 30px; font-size: 12px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'>" +
                        "<div class='success-icon'>✅</div>" +
                        "<h1 style='margin: 0;'>Order Successfully Placed!</h1>" +
                        "<p style='margin: 10px 0 0 0;'>Payment Received</p>" +
                        "</div>" +
                        "<div style='padding: 20px 0;'>" +
                        "<h2 style='color: #333;'>Dear " + teacherName + ",</h2>" +
                        "<p style='color: #666; line-height: 1.6;'>Your order has been confirmed and payment has been received successfully.</p>"
                        +
                        "</div>" +
                        "<div class='order-details'>" +
                        "<h3 style='color: #333; margin-top: 0;'>📋 Order Details:</h3>" +
                        orderDetails +
                        "</div>" +
                        "<div class='total'>" +
                        "💰 Total Amount Paid: ₹" + totalAmount +
                        "</div>" +
                        "<div style='margin-top: 30px; padding: 20px; background: #fff3cd; border-radius: 8px; border-left: 4px solid #ffc107;'>"
                        +
                        "<p style='margin: 0; color: #856404;'><strong>📍 Note:</strong>We will try to deliver your stuffs as early as possible</p>"
                        +
                        "</div>" +
                        "<div class='footer'>" +
                        "<p>Thank you for ordering with us! 🙏</p>" +
                        "<p>— Your Smartconnect Team</p>" +
                        "<p></p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

                message.setContent(emailBody, "text/html; charset=utf-8");

                // Send email
                Transport.send(message);
                Log.d(TAG, "Email sent successfully to: " + teacherEmail);
                return true;

            } catch (Exception e) {
                Log.e(TAG, "Error sending email: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(TAG, "✅ Order confirmation email sent successfully!");
            } else {
                Log.e(TAG, "❌ Failed to send order confirmation email!");
            }
        }
    }
}
