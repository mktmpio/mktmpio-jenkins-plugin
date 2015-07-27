package org.jenkinsci.plugins.mktmpio;

import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildWrapper;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Map;

public class Mktmpio extends SimpleBuildWrapper {

    public static final String DEFAULT_SERVER = "https://mktmp.io";

    // Job config
    private String dbType;
    private boolean shutdownWithBuild = false;

    @DataBoundConstructor
    public Mktmpio(String dbType) {
        this.dbType = dbType;
    }

    static void dispose(final MktmpioEnvironment env, final Launcher launcher, final TaskListener listener) throws IOException, InterruptedException {
        final String instanceID = env.id;
        MktmpioInstance instance = new MktmpioInstance(env);
        instance.destroy();
        listener.getLogger().printf("mktmpio instance shutdown. type: %s, host: %s, port: %d\n", env.dbType, env.host, env.port);
    }

    public String getDbType() {
        return dbType;
    }

    @DataBoundSetter
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public boolean isShutdownWithBuild() {
        return shutdownWithBuild;
    }

    @DataBoundSetter
    public void setShutdownWithBuild(boolean shutdownWithBuild) {
        this.shutdownWithBuild = shutdownWithBuild;
    }

    @Override
    public MktmpioDescriptor getDescriptor() {
        return (MktmpioDescriptor) super.getDescriptor();
    }

    @Override
    public void setUp(SimpleBuildWrapper.Context context,
                      Run<?, ?> build,
                      FilePath workspace,
                      Launcher launcher,
                      TaskListener listener,
                      EnvVars initialEnvironment)
            throws IOException, InterruptedException {
        final MktmpioDescriptor config = getDescriptor();
        final String token = config.getToken();
        final String baseUrl = config.getServer();
        final String dbType = getDbType();
        final MktmpioInstance instance;
        try {
            listener.getLogger().printf("Attempting to create instance (server: %s, token: %s, type: %s)",
                    baseUrl, token.replaceAll(".", "*"), dbType);
            instance = MktmpioInstance.create(baseUrl, token, dbType, isShutdownWithBuild());
        } catch (IOException ex) {
            listener.fatalError("mktmpio: " + ex.getMessage());
            throw new InterruptedException(ex.getMessage());
        }
        final MktmpioEnvironment env = instance.getEnv();
        final Map<String, String> envVars = env.envVars();
        listener.hyperlink(baseUrl + "/i/" + env.id, env.dbType + " instance " + env.id);
        listener.getLogger().printf("mktmpio instance created: %s\n", env.dbType);
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            listener.getLogger().printf("setting %s=%s\n", entry.getKey(), entry.getValue());
        }
        build.addAction(env);
        context.getEnv().putAll(envVars);
        context.setDisposer(new MktmpioDisposer(env));
    }

    public static ListBoxModel supportedDbTypes() {
        ListBoxModel items = new ListBoxModel();
        items.add("MySQL", "mysql");
        items.add("PostgreSQL-9.4", "postgres");
        items.add("PostgreSQL-9.5", "postgres-9.5");
        items.add("Redis", "redis");
        items.add("MongoDB", "mongodb");
        return items;
    }

    @Extension
    public static final class MktmpioDescriptor extends BuildWrapperDescriptor {

        @CopyOnWrite
        private String token, server = Mktmpio.DEFAULT_SERVER;

        public MktmpioDescriptor() {
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) {
            token = formData.getString("token");
            server = formData.optString("server", Mktmpio.DEFAULT_SERVER);
            save();
            return true;
        }

        @Override
        public Mktmpio newInstance(final StaplerRequest req, final JSONObject formData) throws hudson.model.Descriptor.FormException {
            return req.bindJSON(Mktmpio.class, formData);
        }

        public String getToken() {
            return token;
        }

        @DataBoundSetter
        public void setToken(String token) {
            this.token = token;
        }

        public String getServer() {
            return server;
        }

        @DataBoundSetter
        public void setServer(String server) {
            this.server = server;
        }

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public String getDisplayName() {
            return "Create temporary database server for build";
        }

        public ListBoxModel doFillDbTypeItems() {
            return supportedDbTypes();
        }
    }
}
