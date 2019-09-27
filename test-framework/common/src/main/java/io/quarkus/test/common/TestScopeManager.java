package io.quarkus.test.common;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.quarkus.deployment.test.TestScopeSetup;

public class TestScopeManager {

    private static final List<TestScopeSetup> SCOPE_MANAGERS = new ArrayList<>();

    static {
        for (TestScopeSetup i : ServiceLoader.load(TestScopeSetup.class)) {
            SCOPE_MANAGERS.add(i);
        }
    }

    public static void setup(boolean isSubstrateTest) {
        for (TestScopeSetup i : SCOPE_MANAGERS) {
            i.setup(isSubstrateTest);
        }
    }

    public static void tearDown(boolean isSubstrateTest) {
        for (TestScopeSetup i : SCOPE_MANAGERS) {
            i.tearDown(isSubstrateTest);
        }
    }
}
