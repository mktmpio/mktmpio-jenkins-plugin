package org.jenkinsci.plugins.mktmpio;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class MktmpioBuildWrapperTest extends MktmpioBaseTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setServer() {
        getConfig().setMktmpioServer(mockedServer());
    }

    @Test
    public void failWithBadCredentials() throws Exception {
        getConfig().setToken("totally-bad-token");
        final MktmpioBuildWrapper mktmpio = new MktmpioBuildWrapper("redis");
        final FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "failingProject");
        project.getBuildWrappersList().add(mktmpio);
        prepareToRejectUnauthorized("totally-bad-token", "redis");
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause()).get();
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("Error creating redis instance"));
        assertThat(s, containsString("Authentication required"));
        assertThat(s, not(containsString("MKTMPIO_HOST")));
        assertThat(s, not(containsString("mktmpio instance created")));
        assertThat(s, not(containsString("mktmpio instance shutdown")));
    }

    @Test
    public void succeedWithGoodCredentials() throws Exception {
        getConfig().setToken("totally-legit-token");
        final MktmpioBuildWrapper mktmpio = new MktmpioBuildWrapper("redis");
        final FreeStyleProject project = j.jenkins.createProject(FreeStyleProject.class, "basicProject");
        project.getBuildWrappersList().add(mktmpio);
        prepareFakeInstance("totally-legit-token", "redis");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("MKTMPIO_HOST=12.34.56.78"));
        assertThat(s, containsString("MKTMPIO_PORT=54321"));
        assertThat(s, containsString("mktmpio instance created"));
        assertThat(s, containsString("mktmpio instance shutdown"));
    }

    private MktmpioBuildWrapper.MktmpioBuildWrapperDescriptor getConfig() {
        return (MktmpioBuildWrapper.MktmpioBuildWrapperDescriptor) j.jenkins.getDescriptor(MktmpioBuildWrapper.class);
    }
}