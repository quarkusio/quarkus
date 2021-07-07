package io.quarkus.deployment.mutability;

import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.BUILD_SYSTEM_PROPERTIES;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.DEPLOYMENT_LIB;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.LIB;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.QUARKUS;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PersistentAppModel;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.IsolatedDevModeMain;
import io.quarkus.deployment.pkg.steps.JarResultBuildStep;
import io.quarkus.dev.spi.DevModeType;

@SuppressWarnings("Unused")
public class DevModeTask {

    public static Closeable main(Path appRoot) throws Exception {

        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(appRoot.resolve(LIB).resolve(DEPLOYMENT_LIB).resolve(JarResultBuildStep.APPMODEL_DAT)))) {
            Properties buildSystemProperties = new Properties();
            try (InputStream buildIn = Files
                    .newInputStream(appRoot.resolve(QUARKUS).resolve(BUILD_SYSTEM_PROPERTIES))) {
                buildSystemProperties.load(buildIn);
            }

            PersistentAppModel appModel = (PersistentAppModel) in.readObject();

            AppModel existingModel = appModel.getAppModel(appRoot);
            DevModeContext context = createDevModeContext(appRoot, existingModel);

            CuratedApplication bootstrap = QuarkusBootstrap.builder()
                    .setAppArtifact(existingModel.getAppArtifact())
                    .setExistingModel(existingModel)
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.REMOTE_DEV_SERVER)
                    .setBuildSystemProperties(buildSystemProperties)
                    .setBaseName(appModel.getBaseName())
                    .setApplicationRoot(existingModel.getAppArtifact().getPath())
                    .setTargetDirectory(appRoot.getParent())
                    .setBaseClassLoader(DevModeTask.class.getClassLoader())
                    .build().bootstrap();
            Map<String, Object> map = new HashMap<>();
            map.put(DevModeContext.class.getName(), context);
            map.put(IsolatedDevModeMain.APP_ROOT, appRoot);
            map.put(DevModeType.class.getName(), DevModeType.REMOTE_SERVER_SIDE);
            return (Closeable) bootstrap.runInAugmentClassLoader(IsolatedDevModeMain.class.getName(),
                    map);

        }
    }

    private static DevModeContext createDevModeContext(Path appRoot, AppModel appModel) throws IOException {
        DevModeContext context = new DevModeContext();
        extractDevModeClasses(appRoot, appModel, new PostExtractAction() {
            @Override
            public void run(AppArtifact dep, Path moduleClasses, boolean appArtifact) {

                dep.setPath(moduleClasses);
                DevModeContext.ModuleInfo module = new DevModeContext.ModuleInfo.Builder().setAppArtifactKey(dep.getKey())
                        .setName(dep.getArtifactId())
                        .setClassesPath(moduleClasses.toAbsolutePath().toString())
                        .setResourcesOutputPath(moduleClasses.toAbsolutePath().toString())
                        .build();

                if (appArtifact) {
                    context.setApplicationRoot(module);
                } else {
                    context.getAdditionalModules().add(module);
                }
            }
        });
        context.setAbortOnFailedStart(false);
        context.setLocalProjectDiscovery(false);
        return context;

    }

    public static void extractDevModeClasses(Path appRoot, AppModel appModel, PostExtractAction postExtractAction)
            throws IOException {
        Path extracted = appRoot.resolve("dev");
        Files.createDirectories(extracted);
        Map<AppArtifactKey, AppArtifact> userDependencies = new HashMap<>();
        for (AppDependency i : appModel.getUserDependencies()) {
            userDependencies.put(i.getArtifact().getKey(), i.getArtifact());
        }

        //setup the classes that can be hot reloaded
        //this code needs to be kept in sync with the code in IsolatedRemoteDevModeMain
        //todo: look at a better way of doing this
        for (AppArtifactKey i : appModel.getLocalProjectArtifacts()) {
            boolean appArtifact = i.equals(appModel.getAppArtifact().getKey());
            AppArtifact dep = userDependencies.get(i);
            Path moduleClasses = null;
            if (dep == null) {
                //check if this is the application itself
                if (appArtifact) {
                    dep = appModel.getAppArtifact();
                    moduleClasses = extracted.resolve("app");
                }
            } else {
                moduleClasses = extracted.resolve(i.getGroupId()).resolve(i.getArtifactId());
            }
            if (dep == null) {
                //not all local projects are dependencies
                continue;
            }
            IoUtils.createOrEmptyDir(moduleClasses);
            for (Path p : dep.getPaths()) {
                if (Files.isDirectory(p)) {
                    Path moduleTarget = moduleClasses;
                    Files.walkFileTree(p, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            Files.createDirectories(moduleTarget.resolve(p.relativize(dir)));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Path target = moduleTarget.resolve(p.relativize(file));
                            Files.copy(file, target);
                            Files.setLastModifiedTime(target, Files.getLastModifiedTime(file));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            throw exc;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    try (ZipInputStream fs = new ZipInputStream(Files.newInputStream(p))) {
                        ZipEntry entry = fs.getNextEntry();
                        while (entry != null) {
                            Path target = moduleClasses.resolve(entry.getName());
                            if (entry.getName().endsWith("/")) {
                                Files.createDirectories(target);
                            } else {
                                if (!Files.exists(target)) {
                                    try (OutputStream out = Files.newOutputStream(target)) {
                                        IoUtils.copy(out, fs);
                                    }
                                }
                            }

                            entry = fs.getNextEntry();
                        }
                    }
                }
            }
            if (postExtractAction != null) {
                postExtractAction.run(dep, moduleClasses, appArtifact);
            }
        }
    }

    interface PostExtractAction {
        void run(AppArtifact dep, Path moduleClasses, boolean appArtifact);
    }
}
