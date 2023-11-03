package org.acme;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Collections;
import java.util.Map;

public class LibraryTestResource  implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return Collections.emptyMap();
    }

    @Override
    public void stop() {}
}