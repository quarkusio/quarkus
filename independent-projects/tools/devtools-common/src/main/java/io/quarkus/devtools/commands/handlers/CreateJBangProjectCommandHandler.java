package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.commands.CreateJBangProject.CreateJBangProjectKey.NO_JBANG_WRAPPER;
import static io.quarkus.devtools.commands.handlers.CreateProjectCodestartDataConverter.toCodestartData;
import static io.quarkus.devtools.commands.handlers.QuarkusCommandHandlers.computeCoordsFromQuery;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartCatalog;
import io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartProjectInput;
import io.quarkus.devtools.codestarts.jbang.QuarkusJBangCodestartProjectInputBuilder;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey;
import io.quarkus.devtools.commands.CreateProject.CreateProjectKey;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageIcons;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class CreateJBangProjectCommandHandler implements QuarkusCommandHandler {
    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        final Set<String> extensionsQuery = invocation.getValue(CreateProjectKey.EXTENSIONS, Collections.emptySet());
        final List<ArtifactCoords> extensionsToAdd = computeCoordsFromQuery(invocation, extensionsQuery);
        if (extensionsToAdd == null) {
            throw new QuarkusCommandException("Failed to create project because of invalid extensions");
        }

        final ExtensionCatalog catalog = invocation.getExtensionsCatalog();

        final boolean noWrapper = invocation.getValue(NO_JBANG_WRAPPER, false) ||
                invocation.getValue(CreateProjectKey.NO_BUILDTOOL_WRAPPER, false);

        final QuarkusJBangCodestartProjectInputBuilder builder = QuarkusJBangCodestartProjectInput.builder()
                .addExtensions(extensionsToAdd)
                .setNoJBangWrapper(noWrapper)
                .addData(toCodestartData(invocation.getValues()))
                .putData(QuarkusDataKey.QUARKUS_VERSION, invocation.getExtensionsCatalog().getQuarkusCoreVersion());

        if (catalog.getBom() != null) {
            // TODO properly import the BOMs
            final ArtifactCoords firstBom = catalog.getBom();
            builder.putData(QuarkusJBangCodestartCatalog.JBangDataKey.QUARKUS_BOM_GROUP_ID.key(),
                    firstBom.getGroupId())
                    .putData(QuarkusJBangCodestartCatalog.JBangDataKey.QUARKUS_BOM_ARTIFACT_ID.key(),
                            firstBom.getArtifactId())
                    .putData(QuarkusJBangCodestartCatalog.JBangDataKey.QUARKUS_BOM_VERSION.key(),
                            firstBom.getVersion());
        }
        final QuarkusJBangCodestartProjectInput input = builder.build();

        final Path projectDir = invocation.getQuarkusProject().getProjectDirPath();
        try {
            invocation.log().info("-----------");
            if (!extensionsToAdd.isEmpty()) {
                invocation.log().info("selected extensions: \n"
                        + extensionsToAdd.stream().map(e -> "- " + e.getGroupId() + ":" + e.getArtifactId() + "\n")
                                .collect(Collectors.joining()));
            }
            getCatalog(invocation.getQuarkusProject()).createProject(input).generate(projectDir);
            invocation.log()
                    .info("\n-----------\n" + MessageIcons.SUCCESS_ICON + " "
                            + " jbang project has been successfully generated in:\n--> "
                            + invocation.getQuarkusProject().getProjectDirPath().toString() + "\n-----------");
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to create JBang project: " + e.getMessage(), e);
        }
        return QuarkusCommandOutcome.success();
    }

    private QuarkusJBangCodestartCatalog getCatalog(QuarkusProject project) throws IOException {
        return QuarkusJBangCodestartCatalog.fromResourceLoaders(project.getCodestartResourceLoaders());
    }
}
