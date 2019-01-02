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

package org.jboss.shamrock.creator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jboss.shamrock.creator.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreationContext implements AutoCloseable {

    private final AppArtifact appArtifact;
    private final AppArtifactResolver artifactResolver;
    private Path workDir;
    private boolean deleteTmpDir = true;

    private Map<Class<? extends AppCreationPhaseOutcome>, AppCreationPhaseOutcome> phaseOutcomes = new HashMap<>();

    AppCreationContext(AppArtifact appArtifact, AppArtifactResolver artifactResolver) {
        this.appArtifact = appArtifact;
        this.artifactResolver = artifactResolver;
    }

    void setWorkDir(Path workDir) {
        if(workDir != null) {
            deleteTmpDir = false;
            this.workDir = workDir;
        } else {
            this.workDir = null;
            deleteTmpDir = true;
        }
    }

    /**
     * User application artifact.
     *
     * @return  user application artifact
     */
    public AppArtifact getAppArtifact() {
        return appArtifact;
    }

    /**
     * Artifact resolver which can be used to resolve application dependencies.
     *
     * @return  artifact resolver for application dependencies
     */
    public AppArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    /**
     * Creates a directory from a path relative to the creator's work directory.
     *
     * @param names  represents a path relative to the creator's work directory
     * @return  created directory
     * @throws AppCreatorException  in case the directory could not be created
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
     * @param names  represents a path relative to the creator's work directory
     * @return  path object
     */
    public Path getWorkPath(String... names) {
        if(workDir == null) {
            workDir = IoUtils.createRandomTmpDir();
        }
        if(names.length == 0) {
            return workDir;
        }
        Path p = workDir;
        for(String name : names) {
            p = p.resolve(name);
        }
        return p;
    }

    /**
     * Pushes a phase outcome of a specific type.
     *
     * @param type  phase outcome type
     * @param outcome  phase outcome
     * @throws AppCreatorException  in case the outcome could not be accepted
     */
    public <O extends AppCreationPhaseOutcome> void pushOutcome(Class<O> type, O outcome) throws AppCreatorException {
        protectedPushOutcome(type, outcome);
    }

    protected void protectedPushOutcome(Class<? extends AppCreationPhaseOutcome> type, AppCreationPhaseOutcome outcome) throws AppCreatorException {
        if(phaseOutcomes.put(type, outcome) != null) {
            // let's for now be strict about it
            throw new AppCreatorException("Phase outcome of type " + type.getName() + " has already been provided");
        }
    }

    /**
     * Checks whether an outcome of a specific type has been provided.
     *
     * @param type  phase outcome type
     * @return  true if an outcome of the type is already available, otherwise - false
     */
    public boolean isOutcomeAvailable(Class<? extends AppCreationPhaseOutcome> type) {
        return phaseOutcomes.containsKey(type);
    }

    /**
     * Retrieves an outcome of a specific type or throws an error in case
     * no outcome of this type is available.
     *
     * @param type  phase outcome type
     * @return  phase outcome
     * @throws AppCreatorException  in case no outcome of the type is available yet
     */
    public <O extends AppCreationPhaseOutcome> O getOutcome(Class<O> type) throws AppCreatorException {
        @SuppressWarnings("unchecked")
        final O o = (O) phaseOutcomes.get(type);
        if(o == null) {
            throw new AppCreatorException("Outcome of type " + type.getName() + " has not been provided (make sure the corresponding phase is added to the creator)");
        }
        return o;
    }

    @Override
    public void close() throws AppCreatorException {
        if(deleteTmpDir && workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }
}
