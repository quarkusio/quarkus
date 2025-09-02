package io.quarkus.azure.functions.deployment;

import org.jetbrains.annotations.NotNull;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManagerProvider;

public class QuarkusAzureTaskManagerProvider implements AzureTaskManagerProvider {
    @NotNull
    @Override
    public AzureTaskManager getTaskManager() {
        return new QuarkusAzureTaskManager();
    }
}
