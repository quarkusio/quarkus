package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.metadata.MetadataProviderRegistry;

/**
 * Verifies that the {@code MetadataRegistry} discovery works in native mode.
 * <p>
 * In native images, the {@code META-INF/aesh/registry} resource file must be
 * registered so aesh can discover {@code InternalCommandMetadataRegistry} and
 * look up annotation-processor-generated command metadata providers.
 */
@CommandDefinition(name = "metadata-check", description = "Verify metadata registry works")
public class MetadataCheckCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        var provider = MetadataProviderRegistry.getProvider(MetadataCheckCommand.class);
        invocation.println("metadata-provider: " + (provider != null ? "found" : "missing"));
        return CommandResult.SUCCESS;
    }
}
