package org.jenkinsci.plugins.mktmpio;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.io.IOException;

public class MktmpioInstance {
    private final MktmpioEnvironment env;

    public MktmpioInstance(final MktmpioEnvironment env) {
        this.env = env;
    }

    public static MktmpioInstance create(final String urlRoot, final String token, final String type, final boolean shutdownWithBuild) throws IOException, InterruptedException {
        HttpResponse<JsonNode> json;
        try {
            json = Unirest.post(urlRoot + "/api/v1/new/" + type)
                    .header("accept", "application/json")
                    .header("X-Auth-Token", token)
                    .asJson();
        } catch (UnirestException ex) {
            System.err.println("Error creating instance:" + ex.getMessage());
            throw new IOException(ex.getMessage(), ex);
        }
        if (json.getStatus() >= 400) {
            String message = json.getBody().getObject().optString("error", json.getStatusText());
            System.err.println("Used token: " + token);
            System.err.println("error response: " + json.getStatusText());
            System.err.println("response body: " + json.getBody().toString());
            throw new IOException("Error creating " + type + " instance, " + message);
        }
        JSONObject res = json.getBody().getObject();
        String id = res.getString("id");
        String host = res.getString("host");
        int port = res.getInt("port");
        String username = res.optString("username", "");
        String password = res.optString("password", "");
        final MktmpioEnvironment env = new MktmpioEnvironment(token, id, host, port, username, password, type, shutdownWithBuild);
        return new MktmpioInstance(env);
    }

    public MktmpioEnvironment getEnv() {
        return this.env;
    }

    public void destroy() throws IOException {
        try {
            Unirest.delete("https://mktmp.io/api/v1/i/" + env.id)
                    .header("accept", "application/json")
                    .header("X-Auth-Token", env.token)
                    .asJson();
        } catch (UnirestException ex) {
            throw new IOException("Failed to terminate instance " + env.id, ex);
        }
    }
}
