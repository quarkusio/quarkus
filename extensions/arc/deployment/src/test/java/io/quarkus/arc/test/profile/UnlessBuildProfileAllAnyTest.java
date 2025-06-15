package io.quarkus.arc.test.profile;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class UnlessBuildProfileAllAnyTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addAsResource(new StringAsset("quarkus.test.profile=test,build"), "application.properties"));

    @Inject
    SmallRyeConfig config;
    @Inject
    Instance<UnlessBuildProfileBean> unlessBuildProfiles;

    public interface UnlessBuildProfileBean {
        String profile();
    }

    @Test
    void unlessBuildProfile() {
        assertTrue(config.getProfiles().contains("test"));
        assertTrue(config.getProfiles().contains("build"));

        Set<String> unlessProfiles = unlessBuildProfiles.stream().map(UnlessBuildProfileBean::profile).collect(toSet());
        assertEquals(2, unlessProfiles.size());
        assertTrue(unlessProfiles.contains("dev"));
        assertTrue(unlessProfiles.contains("allOf-dev,allOf-test,allOf-build"));
    }

    // Active, the "dev" profile is not active
    @ApplicationScoped
    @UnlessBuildProfile("dev")
    public static class DevBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "dev";
        }
    }

    // Not active, the "test" profile is active
    @ApplicationScoped
    @UnlessBuildProfile("test")
    public static class TestBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "test";
        }
    }

    // Not active, the "test" profile is active
    @ApplicationScoped
    @UnlessBuildProfile(allOf = "test")
    public static class AllOfTestBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-test";
        }
    }

    // Active, the "test" profile is active
    @ApplicationScoped
    @UnlessBuildProfile(anyOf = "test")
    public static class AnyOfTestBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "anyOf-test";
        }
    }

    // Not active, the "build" profile is active
    @ApplicationScoped
    @UnlessBuildProfile("build")
    public static class BuildBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "build";
        }
    }

    // Not active, the "build" profile is active
    @ApplicationScoped
    @UnlessBuildProfile(allOf = "build")
    public static class AllOfBuildBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-build";
        }
    }

    // Not active, the "build" profile is active
    @ApplicationScoped
    @UnlessBuildProfile(anyOf = "build")
    public static class AnyOfBuildBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "anyOf-build";
        }
    }

    // Not Active, both "test" and "build" profiles are active
    @ApplicationScoped
    @UnlessBuildProfile(allOf = { "test", "build" })
    public static class AllOfTestBuildBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-test,allOf-build";
        }
    }

    // Active, the "dev" profile is not active, and it fails the allOf match
    @ApplicationScoped
    @UnlessBuildProfile(allOf = { "dev", "test", "build" })
    public static class AllOfDevTestBuildBean implements UnlessBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-dev,allOf-test,allOf-build";
        }
    }

    // Not active, the "test" and "build" are active, and either profile fail the anyOf match
    @ApplicationScoped
    @UnlessBuildProfile(anyOf = { "dev", "test", "build" })
    public static class AnyOfDevTestBuildBean implements IfBuildProfileAllAnyTest.IfBuildProfileBean {
        @Override
        public String profile() {
            return "anyOf-dev,anyOf-test,anyOf-build";
        }
    }
}
