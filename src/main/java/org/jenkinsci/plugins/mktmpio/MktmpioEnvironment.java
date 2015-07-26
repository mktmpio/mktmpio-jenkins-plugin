package org.jenkinsci.plugins.mktmpio;

import hudson.model.InvisibleAction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MktmpioEnvironment extends InvisibleAction implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String token;
    public final String id;
    public final String host;
    public final int port;
    public final String username;
    public final String password;
    public final String type;
    public final boolean shutdownWithBuild;

    public MktmpioEnvironment(final String token, final String id, final String host, final int port, final String username, final String password, final String type, final boolean shutdownWithBuild) {
        this.token = token;
        this.id = id;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.type = type;
        this.shutdownWithBuild = shutdownWithBuild;
    }

    public Map<String, String> envVars() {
        Map<String, String> vars = new HashMap<String, String>(6);
        vars.put("MKTMPIO_HOST", host);
        vars.put("MKTMPIO_PORT", Integer.toString(port));
        vars.put("MKTMPIO_USERNAME", username);
        vars.put("MKTMPIO_PASSWORD", password);
        vars.put("MKTMPIO_ID", id);
        vars.put("MKTMPIO_TYPE", type);
        return vars;
    }
}
