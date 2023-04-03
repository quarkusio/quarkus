package io.quarkus.info.deployment;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.io.File;
import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
    InfoBuildTimeValuesBuildItem gitInfo(InfoBuildTimeConfig config,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
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
            Date commitDate = new Date(latestCommit.getCommitTime() * 1000L);
            TimeZone commitTimeZone = TimeZone.getDefault();

            boolean addFullInfo = config.git().mode() == InfoBuildTimeConfig.Git.Mode.FULL;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("branch", repository.getBranch());

            Map<String, Object> commit = new LinkedHashMap<>();
            commit.put("id", latestCommit.getName());
            commit.put("time", formatDate(commitDate, commitTimeZone));

            if (addFullInfo) {

                PersonIdent authorIdent = latestCommit.getAuthorIdent();
                commit.put("author", Map.of("time", formatDate(authorIdent.getWhen(), authorIdent.getTimeZone())));

                PersonIdent committerIdent = latestCommit.getCommitterIdent();
                commit.put("committer", Map.of("time", formatDate(committerIdent.getWhen(), committerIdent.getTimeZone())));

                Map<String, String> user = new LinkedHashMap<>();
                user.put("email", authorIdent.getEmailAddress());
                user.put("name", authorIdent.getName());
                commit.put("user", user);

                Map<String, Object> id = new LinkedHashMap<>();
                id.put("full", latestCommit.getName());
                id.put("abbrev", latestCommit.abbreviate(13).name());
                Map<String, String> message = new LinkedHashMap<>();
                message.put("full", latestCommit.getFullMessage().trim());
                message.put("short", latestCommit.getShortMessage().trim());
                id.put("message", message);

                commit.put("id", id);

                data.put("tags", getTags(repository, latestCommit));
            }

            data.put("commit", commit);
            if (addFullInfo) {
                data.put("build", obtainBuildInfo(curateOutcomeBuildItem, repository));
            }

            return new InfoBuildTimeValuesBuildItem("git", data);
        } catch (Exception e) {
            log.debug("Unable to determine git information", e);
            return null;
        }
    }

    private String formatDate(Date date, TimeZone timeZone) {
        return ISO_OFFSET_DATE_TIME.format(
                OffsetDateTime.ofInstant(date.toInstant(), timeZone.toZoneId()));
    }

    private Map<String, Object> obtainBuildInfo(CurateOutcomeBuildItem curateOutcomeBuildItem,
            Repository repository) {
        Map<String, Object> build = new LinkedHashMap<>();

        String userName = repository.getConfig().getString("user", null, "name");
        String userEmail = repository.getConfig().getString("user", null, "email");
        Map<String, String> user = new LinkedHashMap<>();
        user.put("email", userEmail);
        user.put("name", userName);
        build.put("user", user);

        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        ResolvedDependency appArtifact = applicationModel.getAppArtifact();
        build.put("version", appArtifact.getVersion());

        try {
            // this call might be costly - try to avoid it too (similar concept as in GitDataProvider)
            String buildHost = InetAddress.getLocalHost().getHostName();
            build.put("host", buildHost);
        } catch (Exception e) {
            log.debug("Unable to determine localhost name");
        }
        return build;
    }

    public Collection<String> getTags(Repository repo, final ObjectId objectId) throws GitAPIException {
        try (Git git = Git.wrap(repo)) {
            try (RevWalk walk = new RevWalk(repo)) {
                Collection<String> tags = getTags(git, objectId, walk);
                walk.dispose();
                return tags;
            }
        }
    }

    private Collection<String> getTags(final Git git, final ObjectId objectId, final RevWalk finalWalk) throws GitAPIException {
        return git.tagList().call()
                .stream()
                .filter(tagRef -> {
                    try {
                        final RevCommit tagCommit = finalWalk.parseCommit(tagRef.getObjectId());
                        final RevCommit objectCommit = finalWalk.parseCommit(objectId);
                        if (finalWalk.isMergedInto(objectCommit, tagCommit)) {
                            return true;
                        }
                    } catch (Exception ignored) {
                        log.debug(String.format("Failed while getTags [%s] -- ", tagRef));
                    }
                    return false;
                })
                .map(tagRef -> trimFullTagName(tagRef.getName()))
                .collect(Collectors.toList());
    }

    private String trimFullTagName(String tagName) {
        return tagName.replaceFirst("refs/tags/", "");
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
