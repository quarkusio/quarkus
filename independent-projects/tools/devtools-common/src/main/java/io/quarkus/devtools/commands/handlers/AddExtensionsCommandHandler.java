package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.AddExtensions.EXTENSION_MANAGER;
import static io.quarkus.devtools.messagewriter.MessageIcons.NOK_ICON;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.project.extensions.ExtensionInstallPlan;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.devtools.project.extensions.ExtensionManager.InstallResult;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.ExtensionPredicate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

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

        String quarkusVersion = invocation.getExtensionsCatalog().getQuarkusCoreVersion();

        final ExtensionManager extensionManager = invocation.getValue(EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());
        try {
            ExtensionInstallPlan extensionInstallPlan = planInstallation(quarkusVersion, extensionsQuery,
                    invocation.getExtensionsCatalog());
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

    public ExtensionInstallPlan planInstallation(String quarkusCore, Collection<String> keywords,
            ExtensionCatalog catalog) {
        ExtensionInstallPlan.Builder builder = ExtensionInstallPlan.builder();
        boolean multipleKeywords = keywords.size() > 1;
        for (String keyword : keywords) {
            int countColons = StringUtils.countMatches(keyword, ":");
            // Check if it's just groupId:artifactId
            if (countColons == 1) {
                AppArtifactKey artifactKey = AppArtifactKey.fromString(keyword);
                builder.addManagedExtension(new AppArtifactCoords(artifactKey, null));
                continue;
            } else if (countColons > 1) {
                // it's a gav
                builder.addIndependentExtension(AppArtifactCoords.fromString(keyword));
                continue;
            }
            List<Extension> listed = listInternalExtensions(quarkusCore, keyword, catalog.getExtensions());
            if (listed.size() != 1 && multipleKeywords) {
                // No extension found for this keyword. Return empty immediately
                return ExtensionInstallPlan.EMPTY;
            }
            // If it's a pattern allow multiple results
            // See https://github.com/quarkusio/quarkus/issues/11086#issuecomment-666360783
            else if (listed.size() > 1 && !ExtensionPredicate.isPattern(keyword)) {
                throw new MultipleExtensionsFoundException(keyword, listed);
            }
            for (Extension e : listed) {
                String groupId = e.getArtifact().getGroupId();
                String artifactId = e.getArtifact().getArtifactId();
                String version = e.getArtifact().getVersion();
                AppArtifactCoords extensionCoords = new AppArtifactCoords(groupId, artifactId, version);

                boolean managed = false;
                // TODO this is not properly picking the platform BOMs
                for (ExtensionOrigin origin : e.getOrigins()) {
                    if (origin.isPlatform()) {
                        builder.addManagedExtension(extensionCoords);
                        final ArtifactCoords bomCoords = origin.getBom();
                        builder.addPlatform(new AppArtifactCoords(bomCoords.getGroupId(), bomCoords.getArtifactId(),
                                null, "pom", bomCoords.getVersion()));
                        managed = true;
                        break;
                    }
                }
                if (!managed) {
                    builder.addIndependentExtension(extensionCoords);
                }
            }
            // TODO
            //if (!listed.isEmpty()) {
            //    builder.addPlatform(new AppArtifactCoords(catalog.getBomGroupId(), catalog.getBomArtifactId(), null, "pom",
            //            catalog.getBomVersion()));
            //}
        }
        return builder.build();
    }

    private List<Extension> listInternalExtensions(String quarkusCore, String keyword, Collection<Extension> extensions) {
        List<Extension> result = new ArrayList<>();
        ExtensionPredicate predicate = null;
        if (keyword != null && !keyword.isEmpty()) {
            predicate = new ExtensionPredicate(keyword);
        }
        for (Extension extension : extensions) {
            // If no filter is defined, just return the tuple
            if (predicate == null) {
                result.add(extension);
            } else {
                // If there is an exact match, return only this
                if (predicate.isExactMatch(extension)) {
                    return Collections.singletonList(extension);
                } else if (predicate.test(extension)) {
                    result.add(extension);
                }
            }
        }
        return result;
    }

}
