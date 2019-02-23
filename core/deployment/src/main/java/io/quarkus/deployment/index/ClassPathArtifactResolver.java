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

package io.quarkus.deployment.index;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class path based runner that can resolve artifacts from the current class path.
 * <p>
 * This assumes that all artifacts to be resolved have a META-INF/MANIFEST.MF file,
 * and are layed out in the maven repository structure on the file system.
 */
public class ClassPathArtifactResolver implements ArtifactResolver {

    private static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";
    private final List<StoredUrl> pathList = new ArrayList<>();

    public ClassPathArtifactResolver(ClassLoader classLoader) {
        ClassLoader cl = classLoader;
        try {
            Enumeration<URL> res = cl.getResources(META_INF_MANIFEST_MF);
            while (res.hasMoreElements()) {
                URL jarUrl = res.nextElement();
                String path = jarUrl.getPath();
                if (path.startsWith("file:")) {
                    pathList.add(new StoredUrl(
                            Paths.get(new URI(path.substring(0, path.length() - META_INF_MANIFEST_MF.length() - 2)))));
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResolvedArtifact getArtifact(String groupId, String artifactId, String classifier) {
        Pattern filePatten;
        if (classifier == null || classifier.isEmpty()) {
            filePatten = Pattern.compile(artifactId + "-(\\d.*)\\.jar");
        } else {
            filePatten = Pattern.compile(artifactId + "-" + classifier + "-(\\d.*)\\.jar");
        }
        for (StoredUrl url : pathList) {
            Matcher matcher = filePatten.matcher(url.fileName);
            if (matcher.matches()) {
                String[] groupParts = groupId.split("\\.");
                if (url.path.getNameCount() < groupParts.length + 2) {
                    continue;
                }

                boolean matches = true;
                for (int i = 0; i < groupParts.length; ++i) {
                    String up = url.path.getName(url.path.getNameCount() - groupParts.length - 3 + i).toString();
                    if (!up.equals(groupParts[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    try {
                        return new ResolvedArtifact(groupId, artifactId, matcher.group(1), classifier, url.path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
        throw new RuntimeException("Could not resolve artifact " + groupId + ":" + artifactId + ":" + classifier
                + ". Please make sure it is present and contains a META-INF/MANIFEST.MF file. Note that artifacts that are part of the same project may not always be resolvable, in this case you should generate a META-INF/jandex.idx file instead using the Jandex Maven plugin.");
    }

    static class StoredUrl {
        final Path path;
        final String fileName;

        private StoredUrl(Path path) {
            this.path = path;
            this.fileName = path.getFileName().toString();
        }

        @Override
        public String toString() {
            return "StoredUrl{" +
                    "path=" + path +
                    '}';
        }
    }
}
