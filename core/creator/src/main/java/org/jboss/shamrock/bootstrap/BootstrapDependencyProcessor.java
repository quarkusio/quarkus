/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.shamrock.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.jboss.shamrock.creator.util.ZipUtils;

/**
 *
 * @author Alexey Lloubyansky
 */
public class BootstrapDependencyProcessor {

    private static final Logger log = Logger.getLogger(BootstrapDependencyProcessor.class);

    private static final String INJECT_DEPS_SPLIT_EXPR = "\\s*(,|\\s)\\s*";

    public void process(BootstrapDependencyProcessingContext ctx) throws BootstrapDependencyProcessingException {

        if(!ctx.getType().equals("jar")) {
            return;
        }

        final Path path = ctx.getPath();
        final Properties rtProps;
        if(Files.isDirectory(path)) {
            rtProps = resolveDescriptor(path.resolve(BootstrapConstants.DESCRIPTOR_PATH));
        } else {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                rtProps = resolveDescriptor(artifactFs.getPath(BootstrapConstants.DESCRIPTOR_PATH));
            } catch (IOException e) {
                throw new BootstrapDependencyProcessingException("Failed to open file " + path, e);
            }
        }
        if(rtProps == null) {
            return;
        }

        log.info("Processing platform artifact " + ctx.getGroupId() + ':' + ctx.getArtifactId() + ':' + ctx.getClassifier() + ':' + ctx.getType() + ':' + ctx.getVersion());

        String value = rtProps.getProperty(BootstrapConstants.PROP_REPLACE_WITH_DEP);
        if(value != null) {
            replaceWith(ctx, value);
        }

        value = rtProps.getProperty(BootstrapConstants.PROP_INJECT_DEPS);
        if(value != null) {
            final String[] deps = value.split(INJECT_DEPS_SPLIT_EXPR);
            for(String dep : deps) {
                injectChildDependency(ctx, dep);
            }
        }
    }

    private void replaceWith(BootstrapDependencyProcessingContext ctx, String str) throws BootstrapDependencyProcessingException {
        String groupId = null;
        String artifactId = null;
        String classifier = "";
        String type = "jar";
        String version = null;

        int colon = str.indexOf(':');
        final int length = str.length();
        if(colon < 1 || colon == length - 1) {
            illegalDependencyFormat(str);
        }
        groupId = str.substring(0, colon);
        int offset = colon + 1;
        colon = str.indexOf(':', offset);
        if(colon < 0) {
            artifactId = str.substring(offset, length);
        } else {
            if(colon == length - 1) {
                illegalDependencyFormat(str);
            }
            artifactId = str.substring(offset, colon);
            offset = colon + 1;
            colon = str.indexOf(':', offset);
            if(colon < 0) {
                version = str.substring(offset, length);
            } else {
                if(colon == length - 1) {
                    illegalDependencyFormat(str);
                }
                type = str.substring(offset, colon);
                offset = colon + 1;
                colon = str.indexOf(':', offset);
                if(colon < 0) {
                    version = str.substring(offset, length);
                } else {
                    if (colon == length - 1) {
                        illegalDependencyFormat(str);
                    }
                    classifier = type;
                    type = str.substring(offset, colon);
                    version = str.substring(colon + 1);
                }
            }
        }

        if(version == null || version.isEmpty()) {
            version = ctx.getVersion();
            //illegalDependencyFormat(str);
        }

        ctx.replaceWith(groupId, artifactId, classifier, type, version);
    }

    private Properties resolveDescriptor(final Path rtPropsPath) throws BootstrapDependencyProcessingException {
        final Properties rtProps;
        if (!Files.exists(rtPropsPath)) {
            // not a platform artifact
            return null;
        }
        rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(rtPropsPath)) {
            rtProps.load(reader);
        } catch (IOException e) {
            throw new BootstrapDependencyProcessingException("Failed to load ");
        }
        return rtProps;
    }

    private void injectChildDependency(BootstrapDependencyProcessingContext ctx, String str) throws BootstrapDependencyProcessingException {
        String groupId = null;
        String artifactId = null;
        String classifier = "";
        String type = "jar";
        String version = null;

        int colon = str.indexOf(':');
        final int length = str.length();
        if(colon < 1 || colon == length - 1) {
            illegalDependencyFormat(str);
        }
        groupId = str.substring(0, colon);
        int offset = colon + 1;
        colon = str.indexOf(':', offset);
        if(colon < 0) {
            artifactId = str.substring(offset, length);
        } else {
            if(colon == length - 1) {
                illegalDependencyFormat(str);
            }
            artifactId = str.substring(offset, colon);
            offset = colon + 1;
            colon = str.indexOf(':', offset);
            if(colon < 0) {
                version = str.substring(offset, length);
            } else {
                if(colon == length - 1) {
                    illegalDependencyFormat(str);
                }
                type = str.substring(offset, colon);
                offset = colon + 1;
                colon = str.indexOf(':', offset);
                if(colon < 0) {
                    version = str.substring(offset, length);
                } else {
                    if (colon == length - 1) {
                        illegalDependencyFormat(str);
                    }
                    classifier = type;
                    type = str.substring(offset, colon);
                    version = str.substring(colon + 1);
                }
            }
        }

        if(version == null || version.isEmpty()) {
            version = ctx.getVersion();
            //illegalDependencyFormat(str);
        }

        ctx.injectChild(groupId, artifactId, classifier, type, version);
    }

    private static void illegalDependencyFormat(String str) throws BootstrapDependencyProcessingException {
        throw new BootstrapDependencyProcessingException("Bad artifact coordinates " + str
                + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
    }
}
