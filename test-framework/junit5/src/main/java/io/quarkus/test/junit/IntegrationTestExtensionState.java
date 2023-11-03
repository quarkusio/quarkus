package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import io.quarkus.test.common.TestResourceManager;

public class IntegrationTestExtensionState extends QuarkusTestExtensionState {

    private Map<String, String> sysPropRestore;

    public IntegrationTestExtensionState(TestResourceManager testResourceManager, Closeable resource,
            Map<String, String> sysPropRestore) {
        super(testResourceManager, resource);
        this.sysPropRestore = sysPropRestore;
    }

    @Override
    protected void doClose() throws IOException {
        testResourceManager.close();
        resource.close();
        for (Map.Entry<String, String> entry : sysPropRestore.entrySet()) {
            String val = entry.getValue();
            if (val == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), val);
            }
        }
    }
}
