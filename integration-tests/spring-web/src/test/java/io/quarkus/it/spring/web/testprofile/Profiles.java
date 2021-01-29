package io.quarkus.it.spring.web.testprofile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTestProfile;

public final class Profiles {

    private Profiles() {
    }

    public static boolean shouldCheckForProfiles() {
        return "true".equals(System.getProperty("profiles.check"));
    }

    public static class QuarkusTestProfileNoTags implements QuarkusTestProfile {

    }

    public static class QuarkusTestProfileMatchingTag implements QuarkusTestProfile {

        @Override
        public Set<String> tags() {
            return new HashSet<>(Arrays.asList("test2", "test1", "test3"));
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return Collections.singletonList(new TestResourceEntry(DummyTestResource.class));
        }

        public static class DummyTestResource implements QuarkusTestResourceLifecycleManager {

            public static AtomicInteger COUNT = new AtomicInteger(0);

            @Override
            public Map<String, String> start() {
                COUNT.incrementAndGet();
                return Collections.emptyMap();
            }

            @Override
            public void stop() {

            }
        }
    }

    public static class QuarkusTestProfileNonMatchingTag implements QuarkusTestProfile {

        @Override
        public Set<String> tags() {
            return Collections.singleton("test4");
        }
    }
}
