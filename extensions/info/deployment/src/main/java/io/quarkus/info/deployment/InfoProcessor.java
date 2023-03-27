package io.quarkus.info.deployment;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.info.deployment.spi.InfoBuildTimeContributorBuildItem;
import io.quarkus.info.deployment.spi.InfoBuildTimeValuesBuildItem;
import io.quarkus.info.runtime.InfoRecorder;
import io.quarkus.info.runtime.spi.InfoContributor;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

public class InfoProcessor {

    private static final Logger log = Logger.getLogger(InfoProcessor.class);

    @BuildStep(onlyIf = GitInInfoEndpointEnabled.class)
    InfoBuildTimeValuesBuildItem gitInfo(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem) {
        File projectRoot = highestKnownProjectDirectory(curateOutcomeBuildItem, outputTargetBuildItem);
        if (projectRoot == null) {
            log.debug("Unable to determine project directory");
            return null;
        }
        RepositoryBuilder repositoryBuilder = new RepositoryBuilder().findGitDir(projectRoot);
        if (repositoryBuilder.getGitDir() == null) {
            log.debug("Project is not checked in to git");
            return null;
        }
        try (Repository repository = repositoryBuilder.build()) {

            RevCommit latestCommit = new Git(repository).log().setMaxCount(1).call().iterator().next();

            PersonIdent authorIdent = latestCommit.getAuthorIdent();
            Date authorDate = authorIdent.getWhen();
            TimeZone authorTimeZone = authorIdent.getTimeZone();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("branch", repository.getBranch());
            data.put("commit", Map.of(
                    "id", latestCommit.getName(),
                    "time",
                    ISO_OFFSET_DATE_TIME.format(
                            OffsetDateTime.ofInstant(authorDate.toInstant(), authorTimeZone.toZoneId()))));
            return new InfoBuildTimeValuesBuildItem("git", data);
        } catch (Exception e) {
            log.debug("Unable to determine git information", e);
            return null;
        }
    }

    private File highestKnownProjectDirectory(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem) {
        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        WorkspaceModule workspaceModule = applicationModel.getAppArtifact().getWorkspaceModule();
        if (workspaceModule != null) {
            // in this case we know the precise project root
            return workspaceModule.getModuleDir();
        }
        // in this case we will simply use the build directory and let jgit go up the file system to determine the git directory - if any
        return outputTargetBuildItem.getOutputDirectory().toFile();
    }

    @BuildStep(onlyIf = BuildInInfoEndpointEnabled.class)
    InfoBuildTimeValuesBuildItem buildInfo(CurateOutcomeBuildItem curateOutcomeBuildItem, InfoBuildTimeConfig config) {
        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        ResolvedDependency appArtifact = applicationModel.getAppArtifact();
        Map<String, Object> buildData = new LinkedHashMap<>();
        buildData.put("group", appArtifact.getGroupId());
        buildData.put("artifact", appArtifact.getArtifactId());
        buildData.put("version", appArtifact.getVersion());
        buildData.put("time", ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())); // TODO: what is the proper notion of build time?
        return new InfoBuildTimeValuesBuildItem("build", finalBuildData(buildData, config.build()));
    }

    private Map<String, Object> finalBuildData(Map<String, Object> buildData, InfoBuildTimeConfig.Build buildConfig) {
        if (buildConfig.additionalProperties().isEmpty()) {
            return buildData;
        }
        Map<String, Object> result = new LinkedHashMap<>(buildData);
        result.putAll(buildConfig.additionalProperties());
        return result;
    }

    @BuildStep(onlyIf = OsInInfoEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    InfoBuildTimeContributorBuildItem osInfo(InfoRecorder recorder) {
        return new InfoBuildTimeContributorBuildItem(recorder.osInfoContributor());
    }

    @BuildStep(onlyIf = JavaInInfoEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    InfoBuildTimeContributorBuildItem javaInfo(InfoRecorder recorder) {
        return new InfoBuildTimeContributorBuildItem(recorder.javaInfoContributor());
    }

    @BuildStep(onlyIf = InfoEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    RouteBuildItem defineRoute(InfoBuildTimeConfig buildTimeConfig,
            List<InfoBuildTimeValuesBuildItem> buildTimeValues,
            List<InfoBuildTimeContributorBuildItem> contributors,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            InfoRecorder recorder) {
        Map<String, Object> buildTimeInfo = buildTimeValues.stream().collect(
                Collectors.toMap(InfoBuildTimeValuesBuildItem::getName, InfoBuildTimeValuesBuildItem::getValue, (x, y) -> y,
                        LinkedHashMap::new));
        List<InfoContributor> infoContributors = contributors.stream()
                .map(InfoBuildTimeContributorBuildItem::getInfoContributor)
                .collect(Collectors.toList());
        return nonApplicationRootPathBuildItem.routeBuilder()
                .management()
                .route(buildTimeConfig.path())
                .routeConfigKey("quarkus.info.path")
                .handler(recorder.handler(buildTimeInfo, infoContributors))
                .displayOnNotFoundPage()
                .blockingRoute()
                .build();
    }
}
