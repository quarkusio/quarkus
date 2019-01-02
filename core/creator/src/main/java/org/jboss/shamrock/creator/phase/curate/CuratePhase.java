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

package org.jboss.shamrock.creator.phase.curate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.shamrock.creator.AppArtifact;
import org.jboss.shamrock.creator.AppCreationContext;
import org.jboss.shamrock.creator.AppCreationPhase;
import org.jboss.shamrock.creator.AppCreatorException;

/**
 *
 * @author Alexey Loubyansky
 */
public class CuratePhase implements AppCreationPhase {

    @Override
    public void process(AppCreationContext ctx) throws AppCreatorException {

        final AppArtifact appArtifact = ctx.getAppArtifact();
        System.out.println("Curate " + appArtifact);

        final Path appJar = ctx.getArtifactResolver().resolve(appArtifact);

        final Model model;
        try (FileSystem fs = FileSystems.newFileSystem(appJar, null)) {
            final Path pomXml = fs.getPath("META-INF", "maven", appArtifact.getGroupId(), appArtifact.getArtifactId(), "pom.xml");
            if(pomXml == null) {
                throw new AppCreatorException("Failed to located META-INF/maven/" + appArtifact.getGroupId() + "/" + appArtifact.getArtifactId() + "/pom.xml in " + appJar);
            }
            try(BufferedReader reader = Files.newBufferedReader(pomXml)) {
                final MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
                model = xpp3Reader.read(reader);
            } catch (XmlPullParserException e) {
                throw new AppCreatorException("Failed to parse application POM model", e);
            }
        } catch (IOException e) {
            throw new AppCreatorException("Failed to load pom.properties from " + appJar, e);
        }

        final List<Dependency> deps = model.getDependencies();
        for(Dependency dep : deps) {
            final String groupId = dep.getGroupId();
            if(!groupId.equals("org.jboss.shamrock") || "test".equals(dep.getScope())) {
                continue;
            }
            System.out.println("- " + dep);
        }
    }
}
