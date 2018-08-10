package org.icatproject.topcatdaaasplugin;

import org.icatproject.topcatdaaasplugin.httpclient.HttpClient;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Stateless;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


@DependsOn("TrustManagerInstaller")
@Stateless
public class TopcatClient {

    HttpClient httpClient;

    @PostConstruct
    public void init() {
        try {
            Properties properties = new Properties();
            httpClient = new HttpClient(properties.getProperty("topcatUrl") + "/topcat");
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public Boolean isAdmin(String icatUrl, String sessionId) throws Exception {
        Properties properties = new Properties();
        String url = "admin/isValidSession?facilityName=" + properties.getProperty("facilityName") + "&sessionId=" + URLEncoder.encode(sessionId, "UTF8");
        return httpClient.get(url, generateStandardHeaders()).toString().equals("true");
    }


    private Map<String, String> generateStandardHeaders() {
        Map<String, String> out = new HashMap<String, String>();
        out.put("Content-Type", "application/json");
        return out;
    }

}