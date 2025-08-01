package io.quarkus.info.deployment;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.io.File;
import java.net.InetAddress;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.builder.Version;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.info.BuildInfo;
import io.quarkus.info.GitInfo;
import io.quarkus.info.JavaInfo;
import io.quarkus.info.OsInfo;
import io.quarkus.info.deployment.spi.InfoBuildTimeContributorBuildItem;
import io.quarkus.info.deployment.spi.InfoBuildTimeValuesBuildItem;
import io.quarkus.info.runtime.InfoRecorder;
import io.quarkus.info.runtime.spi.InfoContributor;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;

public class InfoProcessor {

    private static final Logger log = Logger.getLogger(InfoProcessor.class);

    @BuildStep(onlyIf = GitInInfoEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void gitInfo(InfoBuildTimeConfig config,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            BuildProducer<InfoBuildTimeValuesBuildItem> valuesProducer,
            BuildProducer<SyntheticBeanBuildItem> beanProducer,
            InfoRecorder recorder) {
        File projectRoot = highestKnownProjectDirectory(curateOutcomeBuildItem, outputTargetBuildItem);
        if (projectRoot == null) {
            log.debug("Unable to determine project directory");
            return;
        }
        RepositoryBuilder repositoryBuilder = new RepositoryBuilder().findGitDir(projectRoot);
        if (repositoryBuilder.getGitDir() == null) {
            log.debug("Project is not checked in to git");
            return;
        }
        try (Repository repository = repositoryBuilder.build();
                Git git = Git.wrap(repository)) {

            RevCommit latestCommit = git.log().setMaxCount(1).call().iterator().next();

            boolean addFullInfo = config.git().mode() == InfoBuildTimeConfig.Git.Mode.FULL;

            Map<String, Object> data = new LinkedHashMap<>();
            String branch = repository.getBranch();
            data.put("branch", branch);

            Map<String, Object> commit = new LinkedHashMap<>();
            String latestCommitId = latestCommit.getName();
            commit.put("id", latestCommitId);
            String latestCommitTime = formatDate(Instant.ofEpochSecond(latestCommit.getCommitTime()), ZoneId.systemDefault());
            commit.put("time", latestCommitTime);

            if (addFullInfo) {

                PersonIdent authorIdent = latestCommit.getAuthorIdent();
                commit.put("author", Map.of("time", formatDate(authorIdent.getWhenAsInstant(), authorIdent.getZoneId())));

                PersonIdent committerIdent = latestCommit.getCommitterIdent();
                commit.put("committer",
                        Map.of("time", formatDate(committerIdent.getWhenAsInstant(), committerIdent.getZoneId())));

                Map<String, String> user = new LinkedHashMap<>();
                user.put("email", authorIdent.getEmailAddress());
                user.put("name", authorIdent.getName());
                commit.put("user", user);

                Map<String, Object> id = new LinkedHashMap<>();
                id.put("full", latestCommitId);
                id.put("abbrev", latestCommit.abbreviate(13).name());
                Map<String, String> message = new LinkedHashMap<>();
                message.put("full", latestCommit.getFullMessage().trim());
                message.put("short", latestCommit.getShortMessage().trim());
                id.put("message", message);

                commit.put("id", id);

                data.put("remote",
                        GitUtil.sanitizeRemoteUrl(git.getRepository().getConfig().getString("remote", "origin", "url")));
                data.put("tags", getTags(git, latestCommit));
            }

            data.put("commit", commit);
            if (addFullInfo) {
                data.put("build", obtainBuildInfo(curateOutcomeBuildItem, repository));
            }

            valuesProducer.produce(new InfoBuildTimeValuesBuildItem("git", data));
            beanProducer.produce(SyntheticBeanBuildItem.configure(GitInfo.class)
                    .supplier(recorder.gitInfoSupplier(branch, latestCommitId, latestCommitTime))
                    .scope(ApplicationScoped.class)
                    .setRuntimeInit()
                    .done());
        } catch (Exception e) {
            log.debug("Unable to determine git information", e);
        }
    }

    private String formatDate(Instant instant, ZoneId zoneId) {
        return ISO_OFFSET_DATE_TIME.format(OffsetDateTime.ofInstant(instant, zoneId));
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

    public Collection<String> getTags(final Git git, final ObjectId objectId) throws GitAPIException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            Collection<String> tags = getTags(git, objectId, walk);
            walk.dispose();
            return tags;
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
    @Record(ExecutionTime.RUNTIME_INIT)
    void buildInfo(CurateOutcomeBuildItem curateOutcomeBuildItem,
            InfoBuildTimeConfig config,
            BuildProducer<InfoBuildTimeValuesBuildItem> valuesProducer,
            BuildProducer<SyntheticBeanBuildItem> beanProducer,
            ApplicationInfoBuildItem infoApplication,
            InfoRecorder recorder) {
        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        ResolvedDependency appArtifact = applicationModel.getAppArtifact();
        Map<String, Object> buildData = new LinkedHashMap<>();
        String name = infoApplication.getName();
        buildData.put("name", name);
        String group = appArtifact.getGroupId();
        buildData.put("group", group);
        String artifact = appArtifact.getArtifactId();
        buildData.put("artifact", artifact);
        String version = appArtifact.getVersion();
        buildData.put("version", version);
        String time = ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
        buildData.put("time", time); // TODO: what is the proper notion of build time?
        String quarkusVersion = Version.getVersion();
        buildData.put("quarkusVersion", quarkusVersion);
        Map<String, Object> data = finalBuildData(buildData, config.build());
        valuesProducer.produce(new InfoBuildTimeValuesBuildItem("build", data));
        beanProducer.produce(SyntheticBeanBuildItem.configure(BuildInfo.class)
                .supplier(recorder.buildInfoSupplier(group, artifact, version, time, quarkusVersion))
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .done());
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
    void osInfo(InfoRecorder recorder,
            BuildProducer<InfoBuildTimeContributorBuildItem> valuesProducer,
            BuildProducer<SyntheticBeanBuildItem> beanProducer) {
        valuesProducer.produce(new InfoBuildTimeContributorBuildItem(recorder.osInfoContributor()));
        beanProducer.produce(SyntheticBeanBuildItem.configure(OsInfo.class)
                .supplier(recorder.osInfoSupplier())
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .done());
    }

    @BuildStep(onlyIf = JavaInInfoEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void javaInfo(InfoRecorder recorder,
            BuildProducer<InfoBuildTimeContributorBuildItem> valuesProducer,
            BuildProducer<SyntheticBeanBuildItem> beanProducer) {
        valuesProducer.produce(new InfoBuildTimeContributorBuildItem(recorder.javaInfoContributor()));
        beanProducer.produce(SyntheticBeanBuildItem.configure(JavaInfo.class)
                .supplier(recorder.javaInfoSupplier())
                .scope(ApplicationScoped.class)
                .setRuntimeInit()
                .done());
    }

    @BuildStep(onlyIf = InfoEndpointEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    RouteBuildItem defineRoute(InfoBuildTimeConfig buildTimeConfig,
            List<InfoBuildTimeValuesBuildItem> buildTimeValues,
            List<InfoBuildTimeContributorBuildItem> contributors,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeanBuildItemBuildProducer,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            InfoRecorder recorder) {

        LinkedHashMap<String, Object> buildTimeInfo = new LinkedHashMap<>();
        for (var bi : buildTimeValues) {
            var key = bi.getName();
            var value = bi.getValue();
            if (buildTimeInfo.containsKey(key)) {
                log.warn("Info key " + key + " contains duplicate values. This can lead to unpredictable values being used");
            }
            buildTimeInfo.put(key, value);
        }
        List<InfoContributor> infoContributors = contributors.stream()
                .map(InfoBuildTimeContributorBuildItem::getInfoContributor)
                .collect(Collectors.toList());

        unremovableBeanBuildItemBuildProducer.produce(UnremovableBeanBuildItem.beanTypes(InfoContributor.class));

        RuntimeValue<Map<String, Object>> finalBuildInfo = recorder.getFinalBuildInfo(buildTimeInfo, infoContributors);

        buildTimeActionProducer.produce(new BuildTimeActionBuildItem().actionBuilder()
                .methodName("getApplicationAndEnvironmentInfo")
                .description(
                        "Information about the environment where this Quarkus application is running. "
                                + "Things like Operating System, Java version Git information and application details is available")
                .runtime(finalBuildInfo).build());

        return RouteBuildItem.newManagementRoute(buildTimeConfig.path())
                .withRoutePathConfigKey("quarkus.info.path")
                .withRequestHandler(recorder.handler(finalBuildInfo))
                .displayOnNotFoundPage("Info")
                .build();
    }
}
