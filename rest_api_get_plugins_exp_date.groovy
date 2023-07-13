import com.atlassian.mail.Email;
import com.atlassian.mail.server.MailServerManager;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.jira.component.ComponentAccessor;
import groovy.json.JsonSlurper
import java.time.*


// Static
Date now = Date.from(ZonedDateTime.now().plusDays(1).toInstant()).clearTime()

// User credentials in format: <username:password> to base64
final String userAuthCred = "<token>"
final String baseURL = ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/rest/plugins/applications/1.0/"

JsonSlurper jsonSlurper = new JsonSlurper()


// GET all plugins
HttpURLConnection getPluginsList = (HttpURLConnection) new URL(baseURL).openConnection();
getPluginsList.setRequestProperty("Authorization", "Basic $userAuthCred")
def pluginsListJsonResponse = getPluginsList.getInputStream().getText()
getPluginsList.disconnect()

// Parse response
def parsedPluginsList = jsonSlurper.parseText(pluginsListJsonResponse)

// Sort and save only "User installed" plugins
def userInstalledPlugins = []
parsedPluginsList.plugins.each { it ->
    if (it.usesLicensing && it.userInstalled)
        userInstalledPlugins.add(it.key)
}

// GET each plugin expiration date
def expiredPluginsList = []
for (it in userInstalledPlugins){

    HttpURLConnection getPluginExpirationDate = (HttpURLConnection) new URL("${baseURL+it}-key/license").openConnection();
    getPluginExpirationDate.setRequestProperty("Authorization", "Basic $userAuthCred")
    def pluginExpDateJsonResponse = getPluginExpirationDate.getInputStream().getText()
    getPluginExpirationDate.disconnect()
    
    // Parse response
    def parsedPluginExpDate = jsonSlurper.parseText(pluginExpDateJsonResponse)
    def expDateUnixtime = parsedPluginExpDate.maintenanceExpiryDate

    Date expDateHumantime = new Date(expDateUnixtime).clearTime()
    if (expDateHumantime == now) expiredPluginsList.add(it)
}

if (expiredPluginsList) {
    //
    def subject = "Скоро закончатся лицензии на плагины JIRA"
    def body = "Список плагинов, которые нужно обновить:<br />"
    expiredPluginsList.each { it ->
        body += "* <b>${it}</b><br />"
    }
    def emailAddr = "<your mail>"

    SMTPMailServer mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer();
    if (mailServer) {
        Email email = new Email(emailAddr);
        email.setSubject(subject);
        email.setBody(body);
        mailServer.send(email);
    }
}

