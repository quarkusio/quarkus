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

package io.quarkus.deployment.builditem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.ApplicationArchive;

//temp class
public final class ApplicationArchivesBuildItem extends SimpleBuildItem {

    private final ApplicationArchive root;
    private final Collection<ApplicationArchive> applicationArchives;

    public ApplicationArchivesBuildItem(ApplicationArchive root, Collection<ApplicationArchive> applicationArchives) {
        this.root = root;
        this.applicationArchives = applicationArchives;
    }

    /**
     * Returns an {@link ApplicationArchive} that represents the classes and resources that are part of the current
     * project
     *
     * @return The root archive
     */
    public ApplicationArchive getRootArchive() {
        return root;
    }

    /**
     * @return A set of all application archives, excluding the root archive
     */
    public Collection<ApplicationArchive> getApplicationArchives() {
        return applicationArchives;
    }

    /**
     * @return A set of all application archives, including the root archive
     */
    public Set<ApplicationArchive> getAllApplicationArchives() {
        HashSet<ApplicationArchive> ret = new HashSet<>(applicationArchives);
        ret.add(root);
        return Collections.unmodifiableSet(ret);
    }

    /**
     * Returns the archive that contains the given class name, or null if the class cannot be found
     *
     * @param className The class name
     * @return The application archive
     */
    public ApplicationArchive containingArchive(String className) {
        DotName name = DotName.createSimple(className);
        if (root.getIndex().getClassByName(name) != null) {
            return root;
        }
        for (ApplicationArchive i : applicationArchives) {
            if (i.getIndex().getClassByName(name) != null) {
                return i;
            }
        }
        return null;
    }

}
