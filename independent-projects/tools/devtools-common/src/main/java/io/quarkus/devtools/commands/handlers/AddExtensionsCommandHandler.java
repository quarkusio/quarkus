package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.AddExtensions.EXTENSION_MANAGER;
import static io.quarkus.devtools.messagewriter.MessageIcons.NOK_ICON;

import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.devtools.project.extensions.ExtensionManager.InstallResult;
import io.quarkus.registry.DefaultExtensionRegistry;
import io.quarkus.registry.ExtensionRegistry;
import io.quarkus.registry.MultipleExtensionsFoundException;
import java.io.IOException;
import java.util.Collections;
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

        ExtensionRegistry extensionRegistry = invocation.getValue(AddExtensions.EXTENSION_REGISTRY);
        if (extensionRegistry == null) {
            extensionRegistry = DefaultExtensionRegistry.fromPlatform(invocation.getPlatformDescriptor());
        }
        String quarkusVersion = invocation.getPlatformDescriptor().getQuarkusVersion();

        final ExtensionManager extensionManager = invocation.getValue(EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());
        try {
            ExtensionInstallPlan extensionInstallPlan = extensionRegistry.planInstallation(quarkusVersion, extensionsQuery);
            if (extensionInstallPlan.isNotEmpty()) {
                final InstallResult result = extensionManager.install(extensionInstallPlan);
                result.getInstalled()
                        .forEach(a -> invocation.log()
                                .info(MessageIcons.OK_ICON + " Extension " + a.getGroupId() + ":" + a.getArtifactId()
                                        + " has been installed"));
                return new QuarkusCommandOutcome(true).setValue(AddExtensions.OUTCOME_UPDATED, result.isSourceUpdated());
            }
        } catch (MultipleExtensionsFoundException m) {
            StringBuilder sb = new StringBuilder();
            sb.append(NOK_ICON + " Multiple extensions matching '").append(m.getKeyword()).append("'");
            m.getExtensions()
                    .forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                            .append(extension.managementKey()));
            sb.append(System.lineSeparator())
                    .append("     Be more specific e.g using the exact name or the full GAV.");
            invocation.log().info(sb.toString());
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to add extensions", e);
        }

        return new QuarkusCommandOutcome(false).setValue(AddExtensions.OUTCOME_UPDATED, false);
    }

}
