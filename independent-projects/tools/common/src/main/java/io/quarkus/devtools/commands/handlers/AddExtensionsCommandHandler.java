package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.AddExtensions.EXTENSION_MANAGER;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.devtools.project.extensions.ExtensionManager.InstallResult;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This class is thread-safe. It extracts extensions to be added to the project from an instance of
 * {@link QuarkusCommandInvocation}.
 */
public class AddExtensionsCommandHandler implements QuarkusCommandHandler {

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final Set<String> extensionsQuery = invocation.getValue(AddExtensions.EXTENSIONS, Collections.emptySet());
        if (extensionsQuery.isEmpty()) {
            return QuarkusCommandOutcome.success().setValue(AddExtensions.OUTCOME_UPDATED, false);
        }

        final List<AppArtifactCoords> extensionsToAdd = computeCoordsFromQuery(invocation, extensionsQuery);
        final ExtensionManager extensionManager = invocation.getValue(EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());
        try {
            if (extensionsToAdd != null) {
                final InstallResult result = extensionManager.install(extensionsToAdd);
                return new QuarkusCommandOutcome(true).setValue(AddExtensions.OUTCOME_UPDATED, result.isSourceUpdated());
            }

        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to add extensions", e);
        }

        return new QuarkusCommandOutcome(false).setValue(AddExtensions.OUTCOME_UPDATED, false);
    }

}
