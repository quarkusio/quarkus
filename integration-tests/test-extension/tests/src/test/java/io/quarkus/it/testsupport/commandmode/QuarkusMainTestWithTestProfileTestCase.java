package io.quarkus.it.testsupport.commandmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(QuarkusMainTestWithTestProfileTestCase.MyTestProfile.class)
public class QuarkusMainTestWithTestProfileTestCase {

    @Test
    @Launch(value = {})
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput())
                .contains("The bean is mocked value");
    }

    public static class MyTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.package.main-class", "test");
        }

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MockedCdiBean.class);
        }
    }

    @Alternative
    @Singleton
    public static class MockedCdiBean implements CdiBean {

        @Override
        public String myMethod() {
            return "mocked value";
        }
    }
}