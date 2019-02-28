package io.quarkus.test.common;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.quarkus.deployment.test.TestResourceProvider;

public class TestInjectionManager {

    private static final List<TestResourceProvider> resourceProviders = new ArrayList<>();

    static {
        for (TestResourceProvider i : ServiceLoader.load(TestResourceProvider.class)) {
            resourceProviders.add(i);
        }
    }

    public static void inject(Object test) {
        for (TestResourceProvider i : resourceProviders) {
            i.inject(test);
        }
    }

}
