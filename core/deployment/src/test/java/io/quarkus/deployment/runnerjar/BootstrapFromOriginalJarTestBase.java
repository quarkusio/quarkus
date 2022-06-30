package io.quarkus.deployment.runnerjar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;

public abstract class BootstrapFromOriginalJarTestBase extends PackageAppTestBase {

    private TsArtifact appJar;
    private List<TsArtifact> wsModules = List.of();
    private Map<String, List<TsArtifact>> profileWsModules = Map.of();

    protected boolean createWorkspace() {
        return false;
    }

    protected void addWorkspaceModule(TsArtifact a) {
        if (wsModules.isEmpty()) {
            wsModules = new ArrayList<>();
        }
        wsModules.add(a);
    }

    protected void addWorkspaceModuleToProfile(TsArtifact a, String profile) {
        if (profileWsModules.isEmpty()) {
            profileWsModules = new LinkedHashMap<>();
        }
        profileWsModules.computeIfAbsent(profile, k -> new ArrayList<>()).add(a);
    }

    @BeforeEach
    public void initAppModel() throws Exception {
        appJar = composeApplication();
        appJar.install(repo);
    }

    protected abstract TsArtifact composeApplication() throws Exception;

    protected QuarkusBootstrap.Builder initBootstrapBuilder() throws Exception {
        final Path ws = workDir.resolve("workspace");
        IoUtils.recursiveDelete(ws);
        IoUtils.mkdirs(ws);

        Path applicationRoot = resolver.resolve(appJar.toArtifact()).getResolvedPaths().getSinglePath();
        final QuarkusBootstrap.Builder bootstrap = QuarkusBootstrap.builder()
                .setApplicationRoot(applicationRoot)
                .setProjectRoot(applicationRoot)
                .setAppModelResolver(resolver)
                .setTest(isBootstrapForTestMode());

        if (createWorkspace()) {
            System.setProperty("basedir", ws.toAbsolutePath().toString());
            final Model appPom = appJar.getPomModel();

            final List<Dependency> moduleDeps = appPom.getDependencies().stream()
                    .filter(d -> d.getGroupId().equals(appPom.getGroupId()) &&
                            (d.getType().isEmpty() || ArtifactCoords.TYPE_JAR.equals(d.getType())))
                    .collect(Collectors.toList());

            final Path appModule;
            final Path appPomXml;
            if (moduleDeps.isEmpty() || appPom.getParent() != null) {
                appModule = ws;
                appPomXml = ws.resolve("pom.xml");
                ModelUtils.persistModel(appPomXml, appPom);
            } else {
                Model parentPom = new Model();
                parentPom.setModelVersion(appPom.getModelVersion());
                parentPom.setPackaging(ArtifactCoords.TYPE_POM);
                parentPom.setGroupId(appPom.getGroupId());
                parentPom.setArtifactId(appPom.getArtifactId() + "-parent");
                parentPom.setVersion(appPom.getVersion());

                Parent parent = new Parent();
                parent.setGroupId(parentPom.getGroupId());
                parent.setArtifactId(parentPom.getArtifactId());
                parent.setVersion(parentPom.getVersion());

                parentPom.getModules().add(appPom.getArtifactId());
                appModule = ws.resolve(appPom.getArtifactId());
                Files.createDirectories(appModule);
                appPom.setParent(parent);
                appPomXml = appModule.resolve("pom.xml");
                ModelUtils.persistModel(appPomXml, appPom);

                final Map<ArtifactKey, String> managedVersions = new HashMap<>();
                collectManagedDeps(appPom, managedVersions);
                for (Dependency moduleDep : moduleDeps) {
                    parentPom.getModules().add(moduleDep.getArtifactId());
                    final String moduleVersion = moduleDep.getVersion() == null
                            ? managedVersions.get(ArtifactKey.gact(moduleDep.getGroupId(), moduleDep.getArtifactId(),
                                    moduleDep.getClassifier(), moduleDep.getType()))
                            : moduleDep.getVersion();
                    Model modulePom = ModelUtils.readModel(resolver
                            .resolve(ArtifactCoords.pom(moduleDep.getGroupId(), moduleDep.getArtifactId(), moduleVersion))
                            .getResolvedPaths().getSinglePath());
                    modulePom.setParent(parent);
                    final Path moduleDir = IoUtils.mkdirs(ws.resolve(modulePom.getArtifactId()));
                    ModelUtils.persistModel(moduleDir.resolve("pom.xml"), modulePom);
                    final Path resolvedJar = resolver.resolve(new GACTV(modulePom.getGroupId(), modulePom.getArtifactId(),
                            moduleDep.getClassifier(), moduleDep.getType(), modulePom.getVersion())).getResolvedPaths()
                            .getSinglePath();
                    final Path moduleTargetDir = moduleDir.resolve("target");
                    ZipUtils.unzip(resolvedJar, moduleTargetDir.resolve("classes"));
                    IoUtils.copy(resolvedJar,
                            moduleTargetDir.resolve(modulePom.getArtifactId() + "-" + modulePom.getVersion() + ".jar"));
                }

                for (TsArtifact module : wsModules) {
                    parentPom.getModules().add(module.getArtifactId());
                    Model modulePom = module.getPomModel();
                    modulePom.setParent(parent);
                    final Path moduleDir = IoUtils.mkdirs(ws.resolve(modulePom.getArtifactId()));
                    ModelUtils.persistModel(moduleDir.resolve("pom.xml"), modulePom);
                    final Path resolvedJar = resolver.resolve(new GACTV(modulePom.getGroupId(), modulePom.getArtifactId(),
                            module.getClassifier(), module.getType(), modulePom.getVersion())).getResolvedPaths()
                            .getSinglePath();
                    final Path moduleTargetDir = moduleDir.resolve("target");
                    ZipUtils.unzip(resolvedJar, moduleTargetDir.resolve("classes"));
                    IoUtils.copy(resolvedJar,
                            moduleTargetDir.resolve(modulePom.getArtifactId() + "-" + modulePom.getVersion() + ".jar"));
                }

                for (Map.Entry<String, List<TsArtifact>> profileModules : profileWsModules.entrySet()) {

                    Profile profile = null;
                    for (Profile p : parentPom.getProfiles()) {
                        if (p.getId().equals(profileModules.getKey())) {
                            profile = p;
                            break;
                        }
                    }
                    if (profile == null) {
                        for (Profile p : appPom.getProfiles()) {
                            if (p.getId().equals(profileModules.getKey())) {
                                profile = p;
                                break;
                            }
                        }
                        if (profile == null) {
                            throw new IllegalStateException(
                                    "Failed to locate profile " + profileModules.getKey() + " in the application POM");
                        }
                        final Profile tmp = new Profile();
                        tmp.setActivation(profile.getActivation());
                        profile = tmp;
                        parentPom.getProfiles().add(profile);
                    }

                    for (TsArtifact a : profileModules.getValue()) {
                        profile.getModules().add(a.getArtifactId());
                        Model modulePom = a.getPomModel();
                        modulePom.setParent(parent);
                        final Path moduleDir = IoUtils.mkdirs(ws.resolve(modulePom.getArtifactId()));
                        ModelUtils.persistModel(moduleDir.resolve("pom.xml"), modulePom);
                        final Path resolvedJar = resolver.resolve(new GACTV(modulePom.getGroupId(), modulePom.getArtifactId(),
                                a.getClassifier(), a.getType(), modulePom.getVersion())).getResolvedPaths()
                                .getSinglePath();
                        final Path moduleTargetDir = moduleDir.resolve("target");
                        ZipUtils.unzip(resolvedJar, moduleTargetDir.resolve("classes"));
                        IoUtils.copy(resolvedJar,
                                moduleTargetDir.resolve(modulePom.getArtifactId() + "-" + modulePom.getVersion() + ".jar"));
                    }
                }

                ModelUtils.persistModel(ws.resolve("pom.xml"), parentPom);
            }

            final Path appOutputDir = IoUtils.mkdirs(appModule.resolve("target"));
            final Path appClassesDir = appOutputDir.resolve("classes");
            ZipUtils.unzip(applicationRoot, appClassesDir);

            final LocalProject appProject = new BootstrapMavenContext(BootstrapMavenContext.config()
                    .setWorkspaceDiscovery(true)
                    .setRootProjectDir(ws)
                    .setCurrentProject(appPomXml.toString()))
                    .getCurrentProject();

            bootstrap.setProjectRoot(appModule)
                    .setTargetDirectory(appOutputDir)
                    .setLocalProjectDiscovery(true)
                    .setAppModelResolver(newAppModelResolver(appProject));
        } else {
            bootstrap.setTargetDirectory(IoUtils.mkdirs(ws.resolve("target")));
        }

        return bootstrap;
    }

    private void collectManagedDeps(Model appPom, Map<ArtifactKey, String> managedVersions)
            throws IOException, AppModelResolverException {
        final List<Dependency> managed = appPom.getDependencyManagement() == null ? List.of()
                : appPom.getDependencyManagement().getDependencies();
        for (Dependency d : managed) {
            managedVersions.put(ArtifactKey.gact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType()),
                    d.getVersion());
            if (d.getType().equals(ArtifactCoords.TYPE_POM) && d.getScope().equals("import")) {
                collectManagedDeps(ModelUtils
                        .readModel(resolver.resolve(ArtifactCoords.pom(d.getGroupId(), d.getArtifactId(), d.getVersion()))
                                .getResolvedPaths().getSinglePath()),
                        managedVersions);
            }
        }
    }
}
