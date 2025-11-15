package io.quarkus.azure.functions.deployment;

import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManagerProvider;

public class QuarkusAzureActionManagerProvider implements AzureActionManagerProvider {
    @Override
    public AzureActionManager getActionManager() {
        return new QuarkusActionManager();
    }
}
