package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.test.common.TestResourceManager;
import io.smallrye.config.SmallRyeConfig;

public class IntegrationTestExtensionState extends QuarkusTestExtensionState {

    private final Map<String, String> sysPropRestore;

    public IntegrationTestExtensionState(TestResourceManager testResourceManager, Closeable resource,
            Runnable clearCallbacks, Map<String, String> sysPropRestore) {
        super(testResourceManager, resource, clearCallbacks);
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
        // recalculate the property names that may have changed with the restore
        ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getLatestPropertyNames();
    }
}
