/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.shamrock.deployment.builditem;

import java.nio.file.Path;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.builder.item.SimpleBuildItem;

/**
 * Represents an additional application resource root that can be hot reloaded
 *
 * TODO: do we actually need to differentiate between this and the main root? Or can we just have
 * one multi build item that represents both?
 */
public final class AdditionalApplicationArchiveBuildItem extends MultiBuildItem {

    private final Path path;

    public AdditionalApplicationArchiveBuildItem(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
