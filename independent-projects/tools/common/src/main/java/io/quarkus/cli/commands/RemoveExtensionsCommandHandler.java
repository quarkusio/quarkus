package io.quarkus.cli.commands;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.dependencies.Extension;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This class is thread-safe. It extracts extensions to be removed from the project from an instance of
 * {@link QuarkusCommandInvocation}.
 */
public class RemoveExtensionsCommandHandler implements QuarkusCommand {

    final static Printer PRINTER = new Printer();

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final Set<String> extensions = invocation.getValue(RemoveExtensions.EXTENSIONS, Collections.emptySet());
        if (extensions.isEmpty()) {
            return QuarkusCommandOutcome.success().setValue(RemoveExtensions.OUTCOME_UPDATED, false);
        }

        boolean updated = false;
        boolean success = true;

        final List<Extension> registry = invocation.getPlatformDescriptor().getExtensions();
        final BuildFile buildFile = invocation.getBuildFile();

        try {
            for (String query : extensions) {

                if (query.contains(":")) {
                    // GAV case.
                    updated = buildFile.removeExtensionAsGAV(query) || updated;
                } else {
                    SelectionResult result = AddExtensionsCommandHandler.select(query, registry, false);
                    if (!result.matches()) {
                        StringBuilder sb = new StringBuilder();
                        // We have 3 cases, we can still have a single candidate, but the match is on label
                        // or we have several candidates, or none
                        Set<Extension> candidates = result.getExtensions();
                        if (candidates.isEmpty()) {
                            // No matches at all.
                            PRINTER.nok(" Cannot find a dependency matching '" + query + "', maybe a typo?");
                            success = false;
                        } else {
                            sb.append(Printer.NOK).append(" Multiple extensions matching '").append(query).append("'");
                            result.getExtensions()
                                    .forEach(extension -> sb.append(System.lineSeparator()).append("     * ")
                                            .append(extension.managementKey()));
                            sb.append(System.lineSeparator())
                                    .append("     Be more specific e.g using the exact name");
                            PRINTER.print(sb.toString());
                            success = false;
                        }
                    } else { // Matches.
                        for (Extension extension : result) {
                            updated = buildFile.removeDependency(invocation.getPlatformDescriptor(), extension) || updated;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to add extensions", e);
        }

        if (updated) {
            try {
                buildFile.close();
            } catch (IOException e) {
                throw new QuarkusCommandException("Failed to update the project", e);
            }
        }

        return new QuarkusCommandOutcome(success).setValue(RemoveExtensions.OUTCOME_UPDATED, updated);
    }
}
