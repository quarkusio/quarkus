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

package io.quarkus.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Creates an example configuration file.
 */
@Mojo(name = "create-example-config", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExampleConfigMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    public ExampleConfigMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File out = new File(project.getBasedir(), "src/main/resources/application.properties");
            out.getParentFile().mkdirs();

            Properties properties = new Properties();
            for (Artifact a : project.getArtifacts()) {
                if (!"jar".equals(a.getType())) {
                    continue;
                }
                try (ZipFile zip = openZipFile(a)) {
                    ZipEntry deps = zip.getEntry("META-INF/quarkus-descriptions.properties");
                    if (deps != null) {
                        try (InputStream in = zip.getInputStream(deps)) {
                            properties.load(in);
                        }
                    }

                }
            }
            StringBuilder sb = new StringBuilder("Example quarkus config\n\n");
            for (Map.Entry<Object, Object> e : new TreeMap<Object, Object>(properties).entrySet()) {
                sb.append("# ")
                        .append(e.getValue().toString().replace("\n", " "))
                        .append("\n#")
                        .append(e.getKey())
                        .append("=\n\n");
            }
            getLog().error("Creating example File at " + out);
            try (FileOutputStream f = new FileOutputStream(out)) {
                f.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate config", e);
        }

    }

    private ZipFile openZipFile(final Artifact a) {
        final File file = a.getFile();
        if (file == null) {
            throw new RuntimeException("No file for Artifact:" + a.toString());
        }
        if (!Files.isReadable(file.toPath())) {
            throw new RuntimeException("File not existing or not allowed for reading: " + file.getAbsolutePath());
        }
        try {
            return new ZipFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Error opening zip stream from artifact: " + a.toString());
        }
    }

}
