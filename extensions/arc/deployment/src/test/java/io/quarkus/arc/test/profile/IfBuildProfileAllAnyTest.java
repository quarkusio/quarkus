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

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class IfBuildProfileAllAnyTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addAsResource(new StringAsset("quarkus.test.profile=test,build,any"), "application.properties"));

    @Inject
    SmallRyeConfig config;
    @Inject
    Instance<IfBuildProfileBean> ifBuildProfiles;

    @Test
    void ifBuildProfile() {
        assertTrue(config.getProfiles().contains("test"));
        assertTrue(config.getProfiles().contains("build"));
        assertTrue(config.getProfiles().contains("any"));

        Set<String> ifProfiles = ifBuildProfiles.stream().map(IfBuildProfileBean::profile).collect(toSet());
        assertEquals(11, ifProfiles.size());
        assertTrue(ifProfiles.contains("test"));
        assertTrue(ifProfiles.contains("allOf-test"));
        assertTrue(ifProfiles.contains("anyOf-test"));
        assertTrue(ifProfiles.contains("build"));
        assertTrue(ifProfiles.contains("allOf-build"));
        assertTrue(ifProfiles.contains("anyOf-build"));
        assertTrue(ifProfiles.contains("allOf-test,allOf-build"));
        assertTrue(ifProfiles.contains("anyOf-dev,anyOf-test,anyOf-build"));
        assertTrue(ifProfiles.contains("allOf-test,anyOf-build"));
        assertTrue(ifProfiles.contains("allOf-test-build,anyOf-any"));
        assertTrue(ifProfiles.contains("allOf-test-build,anyOf-any-dev"));
    }

    public interface IfBuildProfileBean {
        String profile();
    }

    // Not active, the "dev" profile is not active
    @ApplicationScoped
    @IfBuildProfile("dev")
    public static class DevBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "dev";
        }
    }

    // Active, the "test" profile is active (when used as single value is treated as anyOf)
    @ApplicationScoped
    @IfBuildProfile("test")
    public static class TestBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "test";
        }
    }

    // Active, the "test" profile is active, and it is the only one required by allOf
    @ApplicationScoped
    @IfBuildProfile(allOf = "test")
    public static class AllOfTestBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-test";
        }
    }

    // Active, the "test" profile is active
    @ApplicationScoped
    @IfBuildProfile(anyOf = "test")
    public static class AnyOfTestBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "anyOf-test";
        }
    }

    // Active, the "build" profile is active (when used as single value is treated as anyOf)
    @ApplicationScoped
    @IfBuildProfile("build")
    public static class BuildBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "build";
        }
    }

    // Active, the "build" profile is active, and it is the only one required by allOf
    @ApplicationScoped
    @IfBuildProfile(allOf = "build")
    public static class AllOfBuildBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-build";
        }
    }

    // Active, the "build" profile is active, and it is the only one required by allOf
    @ApplicationScoped
    @IfBuildProfile(anyOf = "build")
    public static class AnyOfBuildBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "anyOf-build";
        }
    }

    // Active, both "test" and "build" profiles are active
    @ApplicationScoped
    @IfBuildProfile(allOf = { "test", "build" })
    public static class AllOfTestBuildBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-test,allOf-build";
        }
    }

    // Not active, the "dev" profile is not active
    @ApplicationScoped
    @IfBuildProfile(allOf = { "dev", "test", "build" })
    public static class AllOfDevTestBuildBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-dev,allOf-test,allOf-build";
        }
    }

    // Active, both "test" and "build" profiles are active, only one is required
    @ApplicationScoped
    @IfBuildProfile(anyOf = { "dev", "test", "build" })
    public static class AnyOfDevTestBuildBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "anyOf-dev,anyOf-test,anyOf-build";
        }
    }

    // Not active, missing the "dev" profile in allOf
    @ApplicationScoped
    @IfBuildProfile(allOf = "dev", anyOf = "test")
    public static class AllOfDevAnyOfTestBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-dev,anyOf-test";
        }
    }

    // Active
    @ApplicationScoped
    @IfBuildProfile(allOf = "test", anyOf = "build")
    public static class AllOfTestAnyOfBuildBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-test,anyOf-build";
        }
    }

    // Active
    @ApplicationScoped
    @IfBuildProfile(allOf = { "test", "build" }, anyOf = "any")
    public static class AllOfTestBuildAnyOfAnyBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-test-build,anyOf-any";
        }
    }

    // Active
    @ApplicationScoped
    @IfBuildProfile(allOf = { "test", "build" }, anyOf = { "any", "dev" })
    public static class AllOfTestBuildAnyOfAnyDevBean implements IfBuildProfileBean {
        @Override
        public String profile() {
            return "allOf-test-build,anyOf-any-dev";
        }
    }
}
