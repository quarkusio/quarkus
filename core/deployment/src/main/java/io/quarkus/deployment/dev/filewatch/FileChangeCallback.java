/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.deployment.dev.filewatch;

import java.util.Collection;

/**
 * Callback for file system change events
 *
 * @see FileSystemWatcher
 * @author Stuart Douglas
 */
public interface FileChangeCallback {

    /**
     * Method that is invoked when file system changes are detected.
     *
     * @param changes the file system changes
     */
    void handleChanges(final Collection<FileChangeEvent> changes);

}
