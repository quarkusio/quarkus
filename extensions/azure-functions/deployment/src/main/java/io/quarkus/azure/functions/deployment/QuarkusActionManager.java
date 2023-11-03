package io.quarkus.azure.functions.deployment;

import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

public class QuarkusActionManager extends AzureActionManager {

    public static void register() {
        final QuarkusActionManager am = new QuarkusActionManager();
        register(am);
    }

    @Override
    public <D> void registerAction(Action<D> action) {
    }

    @Override
    public <D> Action<D> getAction(Action.Id<D> id) {
        return null;
    }

    @Override
    public void registerGroup(String id, ActionGroup group) {

    }

    @Override
    public ActionGroup getGroup(String id) {
        return null;
    }
}
