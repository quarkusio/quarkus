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

package org.jboss.shamrock.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jboss.shamrock.dev.DevModeMain;

/**
 * The dev mojo, that runs a shamrock app in a forked process
 * <p>
 */
@Mojo(name = "dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DevMojo extends AbstractMojo {

    private static final String RESOURCES_PROP = "shamrock.undertow.resources";

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${fakereplace}")
    private boolean fakereplace = false;

    @Parameter(defaultValue = "${debug}")
    private String debug;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDir;

    @Parameter(defaultValue = "${jvm.args}")
    private String jvmArgs;


    @Override
    public void execute() throws MojoFailureException {
        try {
            List<String> args = new ArrayList<>();
            args.add("java");
            if (debug != null) {
                args.add("-Xdebug");
                args.add("-Xnoagent");
                args.add("-Djava.compiler=NONE");
                if (debug.equals("client")) {
                    args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=n,suspend=n");
                } else {
                    args.add("-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y");
                }
            }
            if(jvmArgs != null) {
                args.add(jvmArgs);
            }

            for (Resource r : project.getBuild().getResources()) {
                File f = new File(r.getDirectory());
                File servletRes = new File(f, "META-INF/resources");
                if (servletRes.exists()) {
                    args.add("-D" + RESOURCES_PROP + "=" + servletRes.getAbsolutePath());
                    System.out.println("Using servlet resources " + servletRes.getAbsolutePath());
                    break;
                }
            }

            args.add("-XX:TieredStopAtLevel=1");
            //build a class-path string for the base platform
            //this stuff does not change
            StringBuilder classPath = new StringBuilder();
            for (Artifact artifact : project.getArtifacts()) {
                classPath.append(artifact.getFile().getAbsolutePath());
                classPath.append(" ");
            }
            args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
            File wiringClassesDirectory = Files.createTempDirectory("wiring-classes").toFile();
            wiringClassesDirectory.deleteOnExit();

            classPath.append(wiringClassesDirectory.getAbsolutePath() + "/");
            classPath.append(' ');

            if (fakereplace) {
                File target = new File(buildDir, "fakereplace.jar");
                if (!target.exists()) {
                    //this is super yuck, but there does not seen to be an easy way
                    //to get dependency artifacts. Fakereplace must be called fakereplace.jar to work
                    //so we copy it to the target directory
                    URL resource = getClass().getClassLoader().getResource("org/fakereplace/core/Fakereplace.class");
                    if (resource == null) {
                        throw new RuntimeException("Could not determine Fakereplace location");
                    }
                    String filePath = resource.getPath();
                    try (FileInputStream in = new FileInputStream(filePath.substring(5, filePath.lastIndexOf('!')))) {
                        try (FileOutputStream out = new FileOutputStream(target)) {
                            byte[] buffer = new byte[1024];
                            int r;
                            while ((r = in.read(buffer)) > 0) {
                                out.write(buffer, 0, r);
                            }
                        }
                    }
                }
                args.add("-javaagent:" + target.getAbsolutePath());
                args.add("-Dshamrock.fakereplace=true");
            }

            //we also want to add the maven plugin jar to the class path
            //this allows us to just directly use classes, without messing around copying them
            //to the runner jar
            URL classFile = getClass().getClassLoader().getResource(DevModeMain.class.getName().replace('.', '/') + ".class");
            classPath.append(((JarURLConnection) classFile.openConnection()).getJarFileURL().getFile());

            //now we need to build a temporary jar to actually run

            File tempFile = new File(buildDir, project.getArtifactId()+"-dev.jar");
            tempFile.delete();
            tempFile.deleteOnExit();

            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
                out.putNextEntry(new ZipEntry("META-INF/"));
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPath.toString());
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, DevModeMain.class.getName());
                out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                manifest.write(out);
            }
            String resources = null;
            for(Resource i : project.getBuild().getResources()) {
                //todo: support multiple resources dirs for config hot deployment
                resources = i.getDirectory();
                break;
            }

            outputDirectory.mkdirs();

            args.add("-Dshamrock.runner.classes=" + outputDirectory.getAbsolutePath());
            args.add("-Dshamrock.runner.sources=" + sourceDir.getAbsolutePath());
            if(resources != null) {
                args.add("-Dshamrock.runner.resources=" + new File(resources).getAbsolutePath());
            }
            args.add("-jar");
            args.add(tempFile.getAbsolutePath());
            args.add(outputDirectory.getAbsolutePath());
            args.add(wiringClassesDirectory.getAbsolutePath());
            args.add(new File(buildDir, "transformer-cache").getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[0]));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.directory(outputDirectory);
            Process p = pb.start();

            int val = p.waitFor();
        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

}
