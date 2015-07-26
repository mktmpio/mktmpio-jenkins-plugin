package org.jenkinsci.plugins.mktmpio;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class MktmpioBuildWrapperTest extends MktmpioBaseTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void failWithBadCredentials() throws Exception {
        getConfig().setMktmpioServer(mockedServer());
        getConfig().setToken("totally-bad-token");
        final MktmpioBuildWrapper mktmpio = new MktmpioBuildWrapper("redis");
        final FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "basicProject");
        project.getBuildWrappersList().add(mktmpio);
        prepareToRejectUnauthorized("totally-bad-token", "redis");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.err.println("build env: " + build.getCharacteristicEnvVars().toString());
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("Error creating redis instance"));
        assertThat(s, containsString("Authentication required"));
        assertThat(s, not(containsString("mktmpio instance created.")));
        assertThat(s, not(containsString("mktmpio instance shutdown.")));
    }

    @Test
    public void succeedWithGoodCredentials() throws Exception {
        getConfig().setMktmpioServer(mockedServer());
        getConfig().setToken("totally-legit-token");
        final MktmpioBuildWrapper mktmpio = new MktmpioBuildWrapper("redis");
        final FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "basicProject");
        project.getBuildWrappersList().add(mktmpio);
        prepareFakeInstance("totally-legit-token", "redis");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.err.println("build env: " + build.getCharacteristicEnvVars().toString());
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("mktmpio instance created."));
        assertThat(s, containsString("mktmpio instance shutdown."));
    }

    private MktmpioBuildWrapper.MktmpioBuildWrapperDescriptor getConfig() {
        return (MktmpioBuildWrapper.MktmpioBuildWrapperDescriptor) j.jenkins.getDescriptor(MktmpioBuildWrapper.class);
    }
}