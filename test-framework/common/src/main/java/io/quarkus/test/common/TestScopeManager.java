package io.quarkus.test.common;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.quarkus.deployment.test.TestScopeSetup;

public class TestScopeManager {

    private static final List<TestScopeSetup> scopeManagers = new ArrayList<>();

    static {
        for (TestScopeSetup i : ServiceLoader.load(TestScopeSetup.class)) {
            scopeManagers.add(i);
        }
    }

    public static void setup() {
        for (TestScopeSetup i : scopeManagers) {
            i.setup();
        }
    }

    public static void tearDown() {
        for (TestScopeSetup i : scopeManagers) {
            i.tearDown();
        }
    }
}
