package org.jenkinsci.plugins.mktmpio;

import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MktmpioInstanceTest extends MktmpioBaseTest {
    @Test
    public void failsWithBadCredentials() throws Exception {
        MktmpioInstance failed;
        prepareToRejectUnauthorized("fake-token", "redis");
        try {
            failed = MktmpioInstance.create(mockedServer(), "fake-token", "redis", true);
            assertThat("result is null", failed == null);
        } catch (IOException ex) {
            assertThat(ex.getMessage(), containsString("Authentication required"));
        }
    }

    @Test
    public void succeedsWithGoodCredentials() throws Exception {
        prepareFakeInstance("totally-legit-token", "redis");
        MktmpioInstance redis = MktmpioInstance.create(mockedServer(), "totally-legit-token", "redis", true);
        assertThat("result is not null", redis != null);
        assertThat(redis.getEnv().type, is("redis"));
        redis.destroy();
    }
}