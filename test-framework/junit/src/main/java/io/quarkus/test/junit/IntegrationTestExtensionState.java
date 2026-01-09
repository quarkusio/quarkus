package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.test.common.ListeningAddress;
import io.quarkus.test.common.TestResourceManager;
import io.smallrye.config.SmallRyeConfig;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class IntegrationTestExtensionState extends QuarkusTestExtensionState {

    private final Optional<ListeningAddress> listeningAddress;
    private final Map<String, String> sysPropRestore;

    public IntegrationTestExtensionState(
            ValueRegistry valueRegistry,
            TestResourceManager testResourceManager,
            Closeable resource,
            Runnable clearCallbacks,
            Optional<ListeningAddress> listeningAddress,
            Map<String, String> sysPropRestore) {
        super(valueRegistry, testResourceManager, resource, clearCallbacks);
        this.listeningAddress = listeningAddress;
        this.sysPropRestore = sysPropRestore;
    }

    public Optional<ListeningAddress> getListeningAddress() {
        return listeningAddress;
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
