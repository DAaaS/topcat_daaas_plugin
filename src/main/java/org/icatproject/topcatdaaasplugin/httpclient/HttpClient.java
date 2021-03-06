package org.icatproject.topcatdaaasplugin.httpclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class HttpClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private String url;

    public HttpClient(String url) {
        this.url = url;
    }

    public Response get(String offset, Map<String, String> headers) throws Exception {
        return send("GET", offset, headers);
    }

    public Response post(String offset, Map<String, String> headers, String data) throws Exception {
        return send("POST", offset, headers, data);
    }

    public Response delete(String offset, Map<String, String> headers) throws Exception {
        return send("DELETE", offset, headers);
    }

    public Response put(String offset, Map<String, String> headers, String data) throws Exception {
        return send("PUT", offset, headers, data);
    }

    public Response head(String offset, Map<String, String> headers) throws Exception {
        return send("HEAD", offset, headers);
    }

    private Response send(String method, String offset, Map<String, String> headers, String body) throws Exception {
        StringBuilder url = new StringBuilder(this.url + "/" + offset);

        HttpURLConnection connection = null;

        StringBuilder traceInfo = new StringBuilder();

        traceInfo.append("send: ");
        traceInfo.append(method + " ");
        traceInfo.append(this.url + "/" + offset + " ");
        if (!headers.isEmpty()) {
            traceInfo.append(" (headers) ");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                traceInfo.append(" " + entry.getKey() + " - " + entry.getValue());
            }
            traceInfo.append(" ");
        }
        if (body != null) {
            traceInfo.append(" (body) " + body);
        }
//		logger.trace(traceInfo.toString());

        try {
            //Create connection
            connection = (HttpURLConnection) (new URL(url.toString())).openConnection();
            connection.setRequestMethod(method);
            connection.setUseCaches(false);
            connection.setDoInput(true);

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (body != null && (method.equals("POST") || method.equals("PUT"))) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Length", Integer.toString(body.toString().getBytes().length));

                DataOutputStream request = new DataOutputStream(connection.getOutputStream());
                request.writeBytes(body.toString());
                request.close();
            }

            Integer responseCode = connection.getResponseCode();

            Map<String, String> responseHeaders = new HashMap();
            for (String key : connection.getHeaderFields().keySet()) {
                responseHeaders.put(key, connection.getHeaderField(key));
            }

            String responseBody = "";
            try {
                responseBody = inputStreamToString(connection.getInputStream());
            } catch (Exception e1) {
                try {
                    responseBody = inputStreamToString(connection.getErrorStream());
                } catch (Exception e2) {
                }
            }

            traceInfo = new StringBuilder("send (response): ");
            traceInfo.append("(code) " + responseCode.toString());
            if (!responseHeaders.isEmpty()) {
                traceInfo.append("(headers) ");
                for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                    traceInfo.append(" " + entry.getKey() + " - " + entry.getValue());
                }
            }
            if (responseBody != null) {
                traceInfo.append(" (body) " + responseBody);
            }
            //logger.trace(traceInfo.toString());


            return new Response(responseCode, responseHeaders, responseBody);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private Response send(String method, String offset, Map<String, String> headers) throws Exception {
        return send(method, offset, headers, null);
    }

    private String inputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        int currentChar;
        while ((currentChar = bufferedReader.read()) > -1) {
            out.append(Character.toChars(currentChar));
        }
        bufferedReader.close();
        return out.toString();
    }


}