package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.RemoveExtensions.EXTENSION_MANAGER;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;

import io.quarkus.devtools.commands.RemoveExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.devtools.project.extensions.ExtensionManager.UninstallResult;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is thread-safe. It extracts extensions to be removed from the project from an instance of
 * {@link QuarkusCommandInvocation}.
 */
public class RemoveExtensionsCommandHandler implements QuarkusCommandHandler {

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final Set<String> extensionsQuery = invocation.getValue(RemoveExtensions.EXTENSIONS, Collections.emptySet());
        if (extensionsQuery.isEmpty()) {
            return QuarkusCommandOutcome.success().setValue(RemoveExtensions.OUTCOME_UPDATED, false);
        }

        final List<ArtifactCoords> extensionsToRemove = computeCoordsFromQuery(invocation, extensionsQuery);
        if (extensionsToRemove == null) {
            return new QuarkusCommandOutcome(false).setValue(RemoveExtensions.OUTCOME_UPDATED, false);
        }
        final ExtensionManager extensionManager = invocation.getValue(EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());
        try {
            final Set<ArtifactKey> keys = extensionsToRemove.stream().map(ArtifactCoords::getKey)
                    .collect(Collectors.toSet());
            final UninstallResult result = extensionManager.uninstall(keys);
            result.getUninstalled()
                    .forEach(a -> invocation.log()
                            .info(MessageIcons.OK_ICON + " Extension " + a.getGroupId() + ":" + a.getArtifactId()
                                    + " has been uninstalled"));
            return new QuarkusCommandOutcome(true).setValue(RemoveExtensions.OUTCOME_UPDATED, result.isSourceUpdated());
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to remove extensions", e);
        }
    }
}
