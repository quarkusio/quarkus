/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import io.quarkus.creator.config.reader.MappedPropertiesHandler;
import io.quarkus.creator.config.reader.PropertiesConfigReaderException;
import io.quarkus.creator.config.reader.PropertiesHandler;
import io.quarkus.creator.outcome.OutcomeResolver;
import io.quarkus.creator.outcome.OutcomeResolverFactory;
import io.quarkus.creator.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreator implements AutoCloseable {

    public static class Builder {

        @SuppressWarnings("rawtypes")
        private List<AppCreationPhase> phases = Collections.emptyList();
        private Path appJar;
        private Path workDir;
        private AppArtifactResolver artifactResolver;

        private Builder() {
        }

        /**
         * Adds a creation phase to the application creation flow.
         * In the current implementation the phases are processed in the order
         * they are added.
         *
         * <p/>
         * NOTE: if user does not provide any phases, Java ServiceLoader mechanism
         * will be used to load phases from the classpath.
         *
         * @param phase application creation phase
         * @return this builder instance
         */
        public Builder addPhase(AppCreationPhase<?> provider) {
            switch (phases.size()) {
                case 0:
                    phases = Collections.singletonList(provider);
                    break;
                case 1:
                    final AppCreationPhase<?> first = phases.get(0);
                    phases = new ArrayList<>(2);
                    phases.add(first);
                default:
                    phases.add(provider);
            }
            return this;
        }

        /**
         * Work directory used to store various data when processing phases.
         * If it's not set by the user, a temporary directory will be created
         * which will be automatically removed after the application have passed
         * through all the phases necessary to produce the requested outcome.
         *
         * @param p work directory
         * @return this AppCreator instance
         */
        public Builder setWorkDir(Path dir) {
            this.workDir = dir;
            return this;
        }

        /**
         * Artifact resolver which should be used to resolve application
         * dependencies.
         * If artifact resolver is not set by the user, the default one will be
         * created based on the user Maven settings.xml file.
         *
         * @param resolver artifact resolver
         */
        public Builder setArtifactResolver(AppArtifactResolver resolver) {
            this.artifactResolver = resolver;
            return this;
        }

        /**
         *
         * @param appJar application JAR
         * @throws AppCreatorException
         */
        public Builder setAppJar(Path appJar) throws AppCreatorException {
            this.appJar = appJar;
            return this;
        }

        /**
         * Builds an instance of an application creator.
         *
         * @return an instance of an application creator
         * @throws AppCreatorException in case of a failure
         */
        public AppCreator build() throws AppCreatorException {
            final AppCreator target = initAppCreator();
            final OutcomeResolverFactory<AppCreator> resolverFactory = OutcomeResolverFactory.<AppCreator> getInstance();
            @SuppressWarnings("rawtypes")
            final Iterable<AppCreationPhase> i = phases.isEmpty() ? ServiceLoader.load(AppCreationPhase.class) : phases;
            for (AppCreationPhase<?> provider : i) {
                resolverFactory.addProvider(provider);
            }
            target.outcomeResolver = resolverFactory.build();
            return target;
        }

        /**
         * Creates an instance of a properties handler initialized with whatever the user provided to this builder instance.
         * The properties handler is assumed to be used to for reading a properties file which includes application creator
         * and various phases configurations.
         *
         * @return properties handler
         * @throws AppCreatorException in case of a failure
         */
        public PropertiesHandler<AppCreator> getPropertiesHandler() throws AppCreatorException {

            final AppCreator target = initAppCreator();

            @SuppressWarnings("rawtypes")
            final Iterable<AppCreationPhase> i = phases.isEmpty() ? ServiceLoader.load(AppCreationPhase.class) : phases;
            final MappedPropertiesHandler<AppCreator> propsHandler = new MappedPropertiesHandler<AppCreator>() {
                @Override
                public AppCreator getTarget() throws PropertiesConfigReaderException {
                    final OutcomeResolverFactory<AppCreator> resolverFactory = OutcomeResolverFactory
                            .<AppCreator> getInstance();
                    for (AppCreationPhase<?> provider : i) {
                        try {
                            resolverFactory.addProvider(provider);
                        } catch (AppCreatorException e) {
                            throw new PropertiesConfigReaderException("Failed to initialize outcome resolver", e);
                        }
                        map(provider.getConfigPropertyName(), provider.getPropertiesHandler(), (flow, nested) -> {
                        });
                    }
                    target.outcomeResolver = resolverFactory.build();
                    return target;
                }
            }.map("output", (t, value) -> {
                t.setWorkDir(Paths.get(value));
            });

            return propsHandler;
        }

        private AppCreator initAppCreator() throws AppCreatorException {
            final AppCreator target = new AppCreator();
            target.setWorkDir(workDir);
            target.artifactResolver = artifactResolver;
            target.appJar = appJar;
            return target;
        }
    }

    /**
     * Returns an instance of a builder that can be used to initialize an application creator.
     *
     * @return application creator builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private OutcomeResolver<AppCreator> outcomeResolver;
    private AppArtifactResolver artifactResolver;
    private Path appJar;
    private Path workDir;
    private boolean deleteTmpDir = true;
    protected Map<Class<?>, Object> outcomes = new HashMap<>();

    private AppCreator() {
    }

    private void setWorkDir(Path workDir) {
        if (workDir != null) {
            deleteTmpDir = false;
            this.workDir = workDir;
        } else {
            this.workDir = null;
            deleteTmpDir = true;
        }
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
    public AppArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    /**
     * User application JAR file
     *
     * @return user application JAR file
     * @throws AppCreatorException
     */
    public Path getAppJar() throws AppCreatorException {
        return appJar;
    }

    /**
     * Resolve a phase outcome of a specific type. The creator will figure out
     * which phases need to be processed to deliver the result.
     *
     * @param outcomeType type of the outcome to deliver
     * @return resolved phase outcome
     * @throws AppCreatorException in case of a failure
     */
    @SuppressWarnings("unchecked")
    public <T> T resolveOutcome(Class<T> outcomeType) throws AppCreatorException {
        Object o = outcomes.get(outcomeType);
        if (o != null || outcomes.containsKey(outcomeType)) {
            return (T) o;
        }
        outcomeResolver.resolve(this, outcomeType);
        o = outcomes.get(outcomeType);
        if (o != null || outcomes.containsKey(outcomeType)) {
            return (T) o;
        }
        throw new AppCreatorException("Outcome of type " + outcomeType + " has not been provided");
    }

    /**
     * Checks whether an outcome of the type is already available, i.e.
     * whether it has already been resolved using this instance of the creator
     * or pushed by the user.
     *
     * @param outcomeType type of the outcome
     * @return true if the outcome is already available
     */
    public boolean isAvailable(Class<?> outcomeType) {
        return outcomes.containsKey(outcomeType);
    }

    /**
     * Returns an already resolved outcome or null in case the outcome is not available yet.
     *
     * @param outcomeType type of the outcome
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutcome(Class<T> outcomeType) {
        return (T) outcomes.get(outcomeType);
    }

    /**
     * This method simply calls {@link #pushOutcome(Class, Object) pushOutcome(outcome.getClass(), outcome)}
     *
     * @param outcome outcome instance
     * @return this application creator instance
     * @throws AppCreatorException in case an outcome of this type is already available
     */
    @SuppressWarnings("unchecked")
    public <T> AppCreator pushOutcome(T outcome) throws AppCreatorException {
        pushOutcome((Class<T>) outcome.getClass(), outcome);
        return this;
    }

    /**
     * Pushes an outcome of a specific type which can be used by phases that depend on it.
     *
     * @param type type of the outcome
     * @param value outcome instance
     * @return this application creator instance
     * @throws AppCreatorException in case an outcome of this type is already available
     */
    public <T> AppCreator pushOutcome(Class<T> type, T value) throws AppCreatorException {
        if (outcomes.containsKey(type)) {
            throw new AppCreatorException("Outcome of type " + type.getName() + " has already been provided");
        }
        outcomes.put(type, value);
        return this;
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

    /**
     * Creates a path object from path relative to the creator's work directory.
     *
     * @param names represents a path relative to the creator's work directory
     * @return path object
     */
    public Path getWorkPath(String... names) {
        if (workDir == null) {
            workDir = IoUtils.createRandomTmpDir();
        }
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
}
