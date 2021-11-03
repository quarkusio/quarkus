package io.quarkus.bootstrap.jbang;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.aether.repository.RemoteRepository;

public class JBangBuilderImpl {
    public static Map<String, Object> postBuild(Path appClasses, Path pomFile, List<Map.Entry<String, String>> repositories,
            List<Map.Entry<String, Path>> dependencies,
            boolean nativeImage) {
        final MavenArtifactResolver quarkusResolver;
        try {
            final BootstrapMavenContext mvnCtx = new BootstrapMavenContext(BootstrapMavenContext.config()
                    .setCurrentProject(pomFile.getParent().toString()));
            final List<RemoteRepository> remoteRepos = new ArrayList<>(mvnCtx.getRemoteRepositories());

            repositories.forEach(repo -> {
                remoteRepos.add(new RemoteRepository.Builder(repo.getKey(), "default", repo.getValue()).build());
            });

            quarkusResolver = MavenArtifactResolver.builder()
                    .setRepositorySystem(mvnCtx.getRepositorySystem())
                    .setRepositorySystemSession(mvnCtx.getRepositorySystemSession())
                    .setRemoteRepositoryManager(mvnCtx.getRemoteRepositoryManager())
                    .setRemoteRepositories(remoteRepos)
                    .build();
        } catch (BootstrapMavenException e) {
            throw new IllegalStateException("Failed to initialize Quarkus bootstrap Maven resolver", e);
        }

        try {
            Path target = Files.createTempDirectory("quarkus-jbang");
            final ResolvedArtifactDependency appArtifact = new ResolvedArtifactDependency("dev.jbang.user", "quarkus", null,
                    "jar", "999-SNAPSHOT", appClasses);
            final QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                    .setBaseClassLoader(JBangBuilderImpl.class.getClassLoader())
                    .setMavenArtifactResolver(quarkusResolver)
                    .setProjectRoot(pomFile.getParent())
                    .setTargetDirectory(target)
                    .setManagingProject(new GACTV("io.quarkus", "quarkus-bom", "", "pom", getQuarkusVersion()))
                    .setForcedDependencies(dependencies.stream().map(s -> {
                        String[] parts = s.getKey().split(":");
                        Dependency artifact;
                        if (parts.length == 3) {
                            artifact = new ArtifactDependency(parts[0], parts[1], null, ArtifactCoords.TYPE_JAR, parts[2]);
                        } else if (parts.length == 4) {
                            artifact = new ArtifactDependency(parts[0], parts[1], null, parts[2], parts[3]);
                        } else if (parts.length == 5) {
                            artifact = new ArtifactDependency(parts[0], parts[1], parts[3], parts[2], parts[4]);
                        } else {
                            throw new RuntimeException("Invalid artifact " + s.getKey());
                        }
                        //artifact.setPath(s.getValue());
                        return artifact;
                    }).collect(Collectors.toList()))
                    .setAppArtifact(appArtifact)
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.PROD);

            CuratedApplication app = builder
                    .build().bootstrap();

            if (nativeImage) {
                System.setProperty("quarkus.package.type", "native");
            }
            Map<String, Object> output = new HashMap<>();
            app.runInAugmentClassLoader("io.quarkus.deployment.jbang.JBangAugmentorImpl", output);
            return output;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getQuarkusVersion() {
        try (InputStream in = JBangBuilderImpl.class.getClassLoader().getResourceAsStream("quarkus-version.txt")) {
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
