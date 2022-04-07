import com.atlassian.mail.Email;
import com.atlassian.mail.server.MailServerManager;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.jira.component.ComponentAccessor;
import groovy.json.JsonSlurper
import java.time.*


// Static
// Current date in format: "2021-07-01T21:00:00+0000"
Date now = Date.from(ZonedDateTime.now().plusDays(1).toInstant()).clearTime()

// User credentials in format: <username:password> to base64
final String userAuthCred = "<TOKEN>"
// Get base JIRA URL and append REST part
final String baseURL = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/rest/plugins/applications/1.0/installed/jira-software/license"

// JsonSlurper for automatic JSON Response parsing
JsonSlurper jsonSlurper = new JsonSlurper()

// Connect to this URL
// Authorize
// GET application license
// Close session
HttpURLConnection getApplicationLicense = (HttpURLConnection) new URL(baseURL).openConnection();
getApplicationLicense.setRequestProperty("Authorization", "Basic $userAuthCred")
def applicationLicenseJsonResponse = getApplicationLicense.getInputStream().getText()
getApplicationLicense.disconnect()

// Parse response
def parsedApplicationLicense = jsonSlurper.parseText(applicationLicenseJsonResponse)

// Unix-timestamp to Human readable time
Date applicationLicenseExpDate = Date.parse("dd/MMM/yy", parsedApplicationLicense.expiryDateString)

// If expiration date will be on next day, then add it to list
// Send email if we have burning down plugins
if (applicationLicenseExpDate == now) {
    def subject = "<summary text>"
    def body = "<body text>"
    def emailAddr = "<nomail@nomail.ru>"

    SMTPMailServer mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer();
    if (mailServer) {
        Email email = new Email(emailAddr);
        email.setSubject(subject);
        email.setBody(body);
        email.addHeader("Content-Type", "text/html; charset=utf-8")
        mailServer.send(email);
    }
}
