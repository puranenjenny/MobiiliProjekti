package com.example.mobiiliprojekti

import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class EmailServices {
    fun sendEmail(email:String, newPassword:String) {
        try {
            // Defining sender's email and password
            val senderEmail = "pennypaladinapp@gmail.com"
            val password = "yvgl sfqd jgis zyem" //gmail app password

            // Defining the SMTP server host
            val stringHost = "smtp.gmail.com"

            // Setting up email properties
            val properties = System.getProperties()
            properties["mail.smtp.host"] = stringHost
            properties["mail.smtp.port"] = "465"
            properties["mail.smtp.ssl.enable"] = "true"
            properties["mail.smtp.auth"] = "true"

            // Creating a session with authentication
            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, password)
                }
            })

            // Creating a MimeMessage
            val mimeMessage = MimeMessage(session)

            // Adding the recipient's email address
            mimeMessage.addRecipient(Message.RecipientType.TO, InternetAddress(email))

            // Setting the subject and message content
            // You can Specify yours
            mimeMessage.subject = "New password"
            mimeMessage.setText("Here is your new password for Penny Paladin app: $newPassword")

            // Creating a separate thread to send the email
            val t = Thread {
                try {
                    Transport.send(mimeMessage)
                } catch (e: MessagingException) {
                    // Handling messaging exception
                    e.printStackTrace()
                }
            }
            t.start()
        } catch (e: AddressException) {
            // Handling address exception
        } catch (e: MessagingException) {
            // Handling messaging exception (e.g. network error)
        }
    }
}