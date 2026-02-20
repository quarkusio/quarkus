package io.quarkus.deployment.cmd;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A {@link SimpleBuildItem} that represents the aggregate result of all
 * deployment actions.
 * This is used by build tools to report back the overall outcome of the
 * deployment process.
 * <p>
 * It contains a list of {@link DeployCommandActionBuildItem} instances, each
 * representing an individual deployment action and its result.
 *
 * @see DeployCommandActionBuildItem
 * @since 3.8.0
 */
public final class DeployCommandActionResultBuildItem extends SimpleBuildItem {

    /**
     * The list of deployment command actions that were executed during the
     * deployment process.
     */
    private final List<DeployCommandActionBuildItem> commands;

    /**
     * Constructs a new {@link DeployCommandActionResultBuildItem} with the given
     * list of deployment command actions.
     *
     * @param commands the list of deployment command actions that were executed
     */
    public DeployCommandActionResultBuildItem(List<DeployCommandActionBuildItem> commands) {
        this.commands = commands;
    }

    /**
     * Returns the list of deployment command actions that were
     * executed during the deployment process.
     *
     * @return the executed deployment command actions
     */
    public List<DeployCommandActionBuildItem> getCommands() {
        return commands;
    }
}
