/*
 *    Copyright (c) 2016-2018 Red Hat, Inc.
 *
 *    Red Hat licenses this file to you under the Apache License, version
 *    2.0 (the "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *    implied.  See the License for the specific language governing
 *    permissions and limitations under the License.
 */

package org.jboss.shamrock.maven.components;

import com.google.common.base.Strings;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prompt implementation.
 *
 * <strong>Important:</strong> All variable named injected in the templates uses `_` as word separator. Indeed,
 * FreeMarker does not support `-` as it interprets it as "minus".
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Component(role = SetupTemplates.class, instantiationStrategy = "singleton")
public class SetupTemplates {

    private static final Configuration cfg;
    private static final String JAVA_EXTENSION = ".java";

    private Map<String, String> getDefaultContext() {
        Map<String, String> context = new LinkedHashMap<>();
        MojoUtils.getAllProperties().forEach((k, v) -> context.put(k.replace("-", "_"), v));
        return context;
    }

    static {
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setTemplateLoader(new ClassTemplateLoader(SetupTemplates.class, "/"));
    }

    public void createNewProjectPomFile(Map<String, String> context, File pomFile) throws MojoExecutionException {
        Map<String, String> ctx = merge(getDefaultContext(), context);
        try {
            Template temp = cfg.getTemplate("templates/pom-template.ftl");
            Writer out = new FileWriter(pomFile);
            temp.process(ctx, out);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to generate pom.xml", e);
        }
    }

    private Map<String, String> merge(Map<String, String> context, Map<String, String> toMerge) {
        context.putAll(toMerge);
        return context;
    }

    public void generate(MavenProject project, String path, String className, Log log) throws MojoExecutionException {
        if (Strings.isNullOrEmpty(className)) {
            return;
        }
        log.info("Creating resource " + className);

        File root = new File(project.getBasedir(), "src/main/java");
        File testRoot = new File(project.getBasedir(), "src/test/java");

        String packageName = null;
        if (className.endsWith(JAVA_EXTENSION)) {
            className = className.substring(0, className.length() - JAVA_EXTENSION.length());
        }

        if (className.contains(".")) {
            int idx = className.lastIndexOf('.');
            packageName = className.substring(0, idx);
            className = className.substring(idx + 1);
        }

        if (packageName != null) {
            File packageDir = new File(root, packageName.replace('.', '/'));
            File testPackageDir = new File(testRoot, packageName.replace('.', '/'));
            root = mkdirs(packageDir, log);
            testRoot = mkdirs(testPackageDir, log);
        }

        File classFile = new File(root, className + JAVA_EXTENSION);
        File testClassFile = new File(testRoot, className + "Test" + JAVA_EXTENSION);
        Map<String, String> context = new HashMap<>();
<<<<<<< HEAD:maven/src/main/java/org/jboss/shamrock/maven/components/SetupTemplates.java
        context.put("class_name", className);
=======
        context.put("className", className);
        context.put("root_prefix", rootPath);
>>>>>>> merge cli branch to master:cli/maven/src/main/java/org/jboss/shamrock/maven/components/SetupTemplates.java
        context.put("path", path);
        if (packageName != null) {
            context.put("package_name", packageName);
        }
        try {
            Template temp = cfg.getTemplate("templates/resource-template.ftl");
            Writer out = new FileWriter(classFile);
            temp.process(context, out);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to generate resource code", e);
        }

        // Generate test resources.
        try {
            Template temp = cfg.getTemplate("templates/test-resource-template.ftl");
            Writer out = new FileWriter(testClassFile);
            temp.process(context, out);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to generate test code", e);
        }
<<<<<<< HEAD:maven/src/main/java/org/jboss/shamrock/maven/components/SetupTemplates.java
=======

        // Generate application.
        File appClassFile = new File(root, "MyApplication.java");
        try {
            Template temp = cfg.getTemplate("templates/application-template.ftl");
            Writer out = new FileWriter(appClassFile);
            temp.process(context, out);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to generate Application class", e);
        }
>>>>>>> merge cli branch to master:cli/maven/src/main/java/org/jboss/shamrock/maven/components/SetupTemplates.java
    }

    public void createIndexPage(Map<String, String> context, File basedir, Log log) throws MojoExecutionException {
        Map<String, String> ctx = merge(getDefaultContext(), context);
        // Generate index page
        File resources = new File(basedir, "src/main/resources/META-INF/resources");
        File index = new File(mkdirs(resources, log), "index.html");
        if (!index.exists()) {
            try (Writer out = new FileWriter(index)) {
                Template temp = cfg.getTemplate("templates/index.ftl");
                temp.process(ctx, out);
                log.info("Welcome page created in src/main/resources/META-INF/resources/" + index.getName());
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to generate the welcome page", e);
            }
        }

    }

    public void createDockerFile(Map<String, String> context, File basedir, Log log) throws MojoExecutionException {
        Map<String, String> ctx = merge(getDefaultContext(), context);
        File dockerRoot = new File(basedir, "src/main/docker");
        File docker = new File(mkdirs(dockerRoot, log), "Dockerfile");
        try {
            Template temp = cfg.getTemplate("templates/dockerfile.ftl");
            Writer out = new FileWriter(docker);
            temp.process(ctx, out);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to generate the docker file", e);
        }
    }

    public void createConfiguration(File basedir, Log log) throws MojoExecutionException {
        File meta = new File(basedir, "src/main/resources/META-INF");
        File file = new File(mkdirs(meta, log), "microprofile-config.properties");
        if (!file.exists()) {
            try {
                Files.write(file.toPath(), Arrays.asList("# Configuration file", "key = value"), StandardOpenOption.CREATE_NEW);
                log.info("Configuration file created in src/main/resources/META-INF/" + file.getName());
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to generate the configuration file", e);
            }
        }

    }

    private File mkdirs(File dir, Log log) {
        if (! dir.exists()) {
            boolean created = dir.mkdirs();
            log.debug("Directory " + dir.getAbsolutePath() + " created: " + created);
            log.info("Creating directory " + dir.getAbsolutePath());
        }
        return dir;
    }
}
