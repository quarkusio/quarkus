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

package io.quarkus.creator.phase.curate;

import java.util.List;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppArtifactResolver;
import io.quarkus.creator.AppCreatorException;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultUpdateDiscovery implements UpdateDiscovery {

    private final AppArtifactResolver resolver;
    private final VersionUpdateNumber updateNumber;

    public DefaultUpdateDiscovery(AppArtifactResolver resolver, VersionUpdateNumber updateNumber) {
        this.resolver = resolver;
        this.updateNumber = updateNumber;
    }

    @Override
    public List<String> listUpdates(AppArtifact artifact) throws AppCreatorException {
        return resolver.listLaterVersions(artifact, resolveUpToVersion(artifact), false);
    }

    @Override
    public String getNextVersion(AppArtifact artifact) throws AppCreatorException {
        return resolver.getNextVersion(artifact, resolveUpToVersion(artifact), false);
    }

    @Override
    public String getLatestVersion(AppArtifact artifact) throws AppCreatorException {
        /*
         * to control how the versions are compared
         * DefaultArtifactVersion latest = null;
         * String latestStr = null;
         * for (String version : listUpdates(artifact)) {
         * final DefaultArtifactVersion next = new DefaultArtifactVersion(version);
         * if (latest == null || next.compareTo(latest) > 0) {
         * latest = next;
         * latestStr = version;
         * }
         * }
         * return latestStr;
         */
        return resolver.getLatestVersion(artifact, resolveUpToVersion(artifact), false);
    }

    private String resolveUpToVersion(AppArtifact artifact) throws AppCreatorException {
        if (updateNumber == VersionUpdateNumber.MAJOR) {
            return null;
        }

        // here we are looking for the major version which is going to be used
        // as the base for the version range to look for the updates
        final String version = artifact.getVersion();
        final int majorMinorSep = version.indexOf('.');
        if (majorMinorSep <= 0) {
            throw new AppCreatorException("Failed to determine the major version in " + version);
        }
        final String majorStr = version.substring(0, majorMinorSep);
        if (updateNumber == VersionUpdateNumber.MINOR) {
            final long major;
            try {
                major = Long.parseLong(majorStr);
            } catch (NumberFormatException e) {
                throw new AppCreatorException(
                        "The version is expected to start with a number indicating the major version: " + version);
            }
            return String.valueOf(major + 1) + ".alpha";
        }

        final int minorMicroSep = version.indexOf('.', majorMinorSep + 1);
        if (minorMicroSep <= 0) {
            throw new AppCreatorException("Failed to determine the minor version in " + version);
        }
        final String minorStr = version.substring(majorMinorSep + 1, minorMicroSep);
        final long minor;
        try {
            minor = Long.parseLong(minorStr);
        } catch (NumberFormatException e) {
            throw new AppCreatorException(
                    "Failed to parse the minor number in version: " + version);
        }
        return majorStr + "." + String.valueOf(minor + 1) + ".alpha";
    }
}
