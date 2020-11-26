package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog;
import io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartProjectInput;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.project.codegen.ProjectGenerator;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateJBangProjectCommandHandler implements QuarkusCommandHandler {
    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final Set<String> extensionsQuery = invocation.getValue(ProjectGenerator.EXTENSIONS, Collections.emptySet());
        final List<AppArtifactCoords> extensionsToAdd = computeCoordsFromQuery(invocation, extensionsQuery);
        if (extensionsToAdd == null) {
            throw new QuarkusCommandException("Failed to create project because of invalid extensions");
        }

        final QuarkusJBangCodestartProjectInput input = QuarkusJBangCodestartProjectInput.builder()
                .addExtensions(extensionsToAdd)
                .setNoJBangWrapper(invocation.getBooleanValue("noJBangWrapper"))
                .putData("quarkus.version", invocation.getPlatformDescriptor().getBomVersion())
                .build();

        final Path projectDir = invocation.getQuarkusProject().getProjectDirPath();
        try {
            invocation.log().info("-----------");
            if (!extensionsToAdd.isEmpty()) {
                invocation.log().info("selected extensions: \n"
                        + extensionsToAdd.stream().map(e -> "- " + e.getGroupId() + ":" + e.getArtifactId() + "\n")
                                .collect(Collectors.joining()));
            }
            getCatalog(invocation.getPlatformDescriptor()).createProject(input).generate(projectDir);
            invocation.log()
                    .info("\n-----------\n" + MessageIcons.NOOP_ICON
                            + " jbang project has been successfully generated in:\n--> "
                            + invocation.getQuarkusProject().getProjectDirPath().toString() + "\n-----------");
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create JBang project", e);
        }
        return QuarkusCommandOutcome.success();
    }

    private QuarkusJBangCodestartCatalog getCatalog(QuarkusPlatformDescriptor platformDescriptor) throws IOException {
        return QuarkusJBangCodestartCatalog.fromQuarkusPlatformDescriptor(platformDescriptor);
    }
}
