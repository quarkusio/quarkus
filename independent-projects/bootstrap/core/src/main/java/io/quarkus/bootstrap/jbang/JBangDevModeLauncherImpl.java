package io.quarkus.bootstrap.jbang;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * JBang Dev mode entry point.
 * <p>
 * This is launched from the core/launcher module. To avoid any shading issues core/launcher unpacks all its dependencies
 * into the jar file, then uses a custom class loader load them.
 */
public class JBangDevModeLauncherImpl implements Closeable {

    public static Closeable main(String... args) {
        System.clearProperty("quarkus.dev"); //avoid unknown config key warnings
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        URL url = JBangDevModeLauncherImpl.class.getClassLoader().getResource("jbang-dev.dat");
        String jarFilePath = url.getPath().substring(5, url.getPath().indexOf("!"));

        try (DataInputStream contextStream = new DataInputStream(
                JBangDevModeLauncherImpl.class.getClassLoader().getResourceAsStream("jbang-dev.dat"))) {
            String pomContents = contextStream.readUTF();
            Path sourceFile = Paths.get(contextStream.readUTF());
            int depCount = contextStream.readInt();
            Map<String, Path> deps = new HashMap<>();
            for (int i = 0; i < depCount; ++i) {
                String name = contextStream.readUTF();
                Path path = Paths.get(contextStream.readUTF());
                deps.put(name, path);
            }
            Path projectRoot = Files.createTempDirectory("quarkus-jbang");
            try (OutputStream out = Files.newOutputStream(projectRoot.resolve("pom.xml"))) {
                out.write(pomContents.getBytes(StandardCharsets.UTF_8));
            }
            Path targetClasses = projectRoot.resolve("target/classes");
            Files.createDirectories(targetClasses);

            try (ZipFile fz = new ZipFile(new File(jarFilePath))) {
                Enumeration<? extends ZipEntry> entries = fz.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Path path = targetClasses.resolve(entry.getName()).normalize();
                    if (!path.startsWith(targetClasses)) {
                        throw new IOException("Bad ZIP entry: " + path);
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(path);
                    } else {
                        Files.createDirectories(path.getParent());
                        Files.copy(fz.getInputStream(entry), path);
                        Files.setLastModifiedTime(path, entry.getLastModifiedTime());
                    }
                }
            }

            Path srcDir = projectRoot.resolve("src/main/java");
            Files.createDirectories(srcDir);
            Files.createSymbolicLink(srcDir.resolve(sourceFile.getFileName().toString()), sourceFile);
            final LocalProject currentProject = LocalProject.loadWorkspace(projectRoot);
            final ResolvedDependency appArtifact = ResolvedDependencyBuilder.newInstance()
                    .setCoords(currentProject.getAppArtifact(ArtifactCoords.TYPE_JAR))
                    .setResolvedPath(targetClasses)
                    .setWorkspaceModule(currentProject.toWorkspaceModule())
                    .build();

            //todo : proper support for everything
            final QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setBaseClassLoader(JBangDevModeLauncherImpl.class.getClassLoader())
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.DEV)
                    .setTargetDirectory(targetClasses)
                    .setAppArtifact(appArtifact)
                    .setManagingProject(new GACTV("io.quarkus", "quarkus-bom", "", "pom", getQuarkusVersion()))
                    .setForcedDependencies(deps.entrySet().stream().map(s -> {
                        String[] parts = s.getKey().split(":");
                        Dependency artifact;
                        if (parts.length == 3) {
                            artifact = new ArtifactDependency(parts[0], parts[1], null, ArtifactCoords.TYPE_JAR, parts[2]);
                        } else if (parts.length == 4) {
                            artifact = new ArtifactDependency(parts[0], parts[1], null, parts[2], parts[3]);
                        } else if (parts.length == 5) {
                            artifact = new ArtifactDependency(parts[0], parts[1], parts[3], parts[2], parts[4]);
                        } else {
                            throw new RuntimeException("Invalid artifact " + s);
                        }
                        //artifact.setPath(s.getValue());
                        return artifact;
                    }).collect(Collectors.toList()))
                    .setApplicationRoot(targetClasses)
                    .setProjectRoot(projectRoot);

            Map<String, Object> context = new HashMap<>();
            context.put("app-project", currentProject);
            context.put("args", args);
            context.put("app-classes", targetClasses);

            final BootstrapMavenContext mvnCtx = new BootstrapMavenContext(
                    BootstrapMavenContext.config().setCurrentProject(currentProject));
            final MavenArtifactResolver mvnResolver = new MavenArtifactResolver(mvnCtx);
            builder.setMavenArtifactResolver(mvnResolver);
            currentProject.getAppArtifact("jar").setPath(targetClasses);

            final CuratedApplication curatedApp = builder.build().bootstrap();
            final Object appInstance = curatedApp.runInAugmentClassLoader("io.quarkus.deployment.dev.IDEDevModeMain", context);
            return new JBangDevModeLauncherImpl(curatedApp,
                    appInstance == null ? null : appInstance instanceof Closeable ? (Closeable) appInstance : null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final CuratedApplication curatedApp;
    private final Closeable runningApp;

    private JBangDevModeLauncherImpl(CuratedApplication curatedApp, Closeable runningApp) {
        this.curatedApp = curatedApp;
        this.runningApp = runningApp;
    }

    @Override
    public void close() throws IOException {
        try {
            if (runningApp != null) {
                runningApp.close();
            }
        } finally {
            if (curatedApp != null) {
                curatedApp.close();
            }
        }
    }

    private static String getQuarkusVersion() {
        try (InputStream in = JBangDevModeLauncherImpl.class.getClassLoader().getResourceAsStream("quarkus-version.txt")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[10];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
