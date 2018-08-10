package org.icatproject.topcatdaaasplugin;

import org.icatproject.topcatdaaasplugin.httpclient.HttpClient;

import javax.json.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.io.StringReader;


public class IcatClient {

    private HttpClient httpClient;
    private String sessionId;

    public IcatClient(String icatUrl, String sessionId) {
        try {
            this.httpClient = new HttpClient(icatUrl + "/icat");
            this.sessionId = sessionId;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public JsonArray query(String query) throws Exception {
        JsonArrayBuilder out = Json.createArrayBuilder();
        String url = "entityManager?sessionId=" + URLEncoder.encode(sessionId, "UTF8") + "&query=" + URLEncoder.encode(query, "UTF8");
        for (JsonValue jsonObjectValue : parseJsonArray(httpClient.get(url, generateStandardHeaders()).toString())) {
            JsonObject jsonObject = (JsonObject) jsonObjectValue;
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                out.add(entry.getValue());
            }
        }
        return out.build();
    }

    public String getUserName() throws Exception {
        String url = "session/" + URLEncoder.encode(sessionId, "UTF8");
        JsonReader jsonReader = Json.createReader(new StringReader(httpClient.get(url, generateStandardHeaders()).toString()));
        JsonObject session = jsonReader.readObject();
        jsonReader.close();
        return session.getString("userName");
    }

    private Map<String, String> generateStandardHeaders() {
        Map<String, String> out = new HashMap<String, String>();
        out.put("Content-Type", "application/json");
        return out;
    }

    private JsonArray parseJsonArray(String json) throws Exception {
        InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonReader jsonReader = Json.createReader(jsonInputStream);
        JsonArray out = jsonReader.readArray();
        jsonReader.close();
        return out;
    }

}