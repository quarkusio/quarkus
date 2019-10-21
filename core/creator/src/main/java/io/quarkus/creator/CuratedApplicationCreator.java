package io.quarkus.creator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.creator.curator.CurateOutcome;
import io.quarkus.creator.curator.Curator;

/**
 *
 * @author Alexey Loubyansky
 */
public class CuratedApplicationCreator implements AutoCloseable {

    /**
     * Returns an instance of a builder that can be used to initialize an application creator.
     *
     * @return application creator builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private final AppModelResolver artifactResolver;
    private final AppArtifact appArtifact;
    private final Path workDir;
    private boolean deleteTmpDir = true;

    private final DependenciesOrigin depsOrigin;
    private final VersionUpdate update;
    private final VersionUpdateNumber updateNumber;
    private final Path localRepo;
    private final String baseName;

    private CuratedApplicationCreator(Builder builder) {
        this.artifactResolver = builder.modelResolver;
        this.appArtifact = builder.appArtifact;
        this.depsOrigin = builder.depsOrigin;
        this.update = builder.update;
        this.updateNumber = builder.updateNumber;
        this.localRepo = builder.localRepo;
        boolean del;
        if (builder.workDir != null) {
            del = false;
            this.workDir = builder.workDir;
        } else {
            try {
                this.workDir = Files.createTempDirectory("quarkus-build");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            del = true;
        }
        deleteTmpDir = del;

        String finalName = builder.baseName;
        if (finalName == null && appArtifact != null && appArtifact.getPath() != null) {
            final String name = toUri(appArtifact.getPath().getFileName());
            int i = name.lastIndexOf('.');
            if (i > 0) {
                finalName = name.substring(0, i);
            }
        }
        this.baseName = finalName;
    }

    /**
     * Work directory used by the phases to store various data.
     *
     * @return work dir
     */
    public Path getWorkDir() {
        return workDir;
    }

    /**
     * Artifact resolver which can be used to resolve application dependencies.
     *
     * @return artifact resolver for application dependencies
     */
    public AppModelResolver getArtifactResolver() {
        return artifactResolver;
    }

    /**
     * User application JAR file
     *
     * @return user application JAR file
     * @throws AppCreatorException
     */
    public AppArtifact getAppArtifact() throws AppCreatorException {
        return appArtifact;
    }

    public DependenciesOrigin getDepsOrigin() {
        return depsOrigin;
    }

    public VersionUpdate getUpdate() {
        return update;
    }

    public VersionUpdateNumber getUpdateNumber() {
        return updateNumber;
    }

    public Path getLocalRepo() {
        return localRepo;
    }

    public String getBaseName() {
        return baseName;
    }

    /**
     * Creates a directory from a path relative to the creator's work directory.
     *
     * @param names represents a path relative to the creator's work directory
     * @return created directory
     * @throws AppCreatorException in case the directory could not be created
     */
    public Path createWorkDir(String... names) throws AppCreatorException {
        final Path p = getWorkPath(names);
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            throw new AppCreatorException("Failed to create directory " + p, e);
        }
        return p;
    }

    public <T> T runTask(CuratedTask<T> task) throws AppCreatorException {
        CurateOutcome curateResult = Curator.run(this);
        return task.run(curateResult, this);
    }

    /**
     * Creates a path object from path relative to the creator's work directory.
     *
     * @param names represents a path relative to the creator's work directory
     * @return path object
     */
    public Path getWorkPath(String... names) {
        if (names.length == 0) {
            return workDir;
        }
        Path p = workDir;
        for (String name : names) {
            p = p.resolve(name);
        }
        return p;
    }

    @Override
    public void close() {
        if (deleteTmpDir) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    private static StringBuilder toUri(StringBuilder b, Path path, int seg) {
        b.append(path.getName(seg));
        if (seg < path.getNameCount() - 1) {
            b.append('/');
            toUri(b, path, seg + 1);
        }
        return b;
    }

    private static String toUri(Path path) {
        if (path.isAbsolute()) {
            return path.toUri().getPath();
        } else if (path.getNameCount() == 0) {
            return "";
        } else {
            return toUri(new StringBuilder(), path, 0).toString();
        }
    }

    public static class Builder {

        public String baseName;
        private AppArtifact appArtifact;
        private Path workDir;
        private AppModelResolver modelResolver;

        private DependenciesOrigin depsOrigin = DependenciesOrigin.APPLICATION;
        private VersionUpdate update = VersionUpdate.NONE;
        private VersionUpdateNumber updateNumber = VersionUpdateNumber.MICRO;
        private Path localRepo;

        private Builder() {
        }

        public Builder setBaseName(String baseName) {
            this.baseName = baseName;
            return this;
        }

        /**
         * Work directory used to store various data when processing phases.
         * If it's not set by the user, a temporary directory will be created
         * which will be automatically removed after the application have passed
         * through all the phases necessary to produce the requested outcome.
         *
         * @param dir work directory
         * @return this AppCreator instance
         */
        public Builder setWorkDir(Path dir) {
            this.workDir = dir;
            return this;
        }

        /**
         * Application model resolver which should be used to resolve
         * application dependencies.
         * If artifact resolver is not set by the user, the default one will be
         * created based on the user Maven settings.xml file.
         *
         * @param resolver artifact resolver
         */
        public Builder setModelResolver(AppModelResolver resolver) {
            this.modelResolver = resolver;
            return this;
        }

        /**
         *
         * @param appArtifact application JAR
         * @throws AppCreatorException
         */
        public Builder setAppArtifact(AppArtifact appArtifact) {
            this.appArtifact = appArtifact;
            return this;
        }

        public Builder setAppArtifact(Path path) throws AppCreatorException {
            try {
                this.appArtifact = ModelUtils.resolveAppArtifact(path);
                this.appArtifact.setPath(path);
            } catch (IOException e) {
                throw new AppCreatorException("Unable to resolve app artifact " + path);
            }
            return this;
        }

        public Builder setDepsOrigin(DependenciesOrigin depsOrigin) {
            this.depsOrigin = depsOrigin;
            return this;
        }

        public Builder setUpdate(VersionUpdate update) {
            this.update = update;
            return this;
        }

        public Builder setUpdateNumber(VersionUpdateNumber updateNumber) {
            this.updateNumber = updateNumber;
            return this;
        }

        public Builder setLocalRepo(Path localRepo) {
            this.localRepo = localRepo;
            return this;
        }

        /**
         * Builds an instance of an application creator.
         *
         * @return an instance of an application creator
         * @throws AppCreatorException in case of a failure
         */
        public CuratedApplicationCreator build() throws AppCreatorException {
            return new CuratedApplicationCreator(this);
        }
    }
}
