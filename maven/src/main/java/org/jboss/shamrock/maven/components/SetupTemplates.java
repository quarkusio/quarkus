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
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Prompt implementation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Component(role = SetupTemplates.class, instantiationStrategy = "singleton")
public class SetupTemplates {

    private static final Configuration cfg;
    private static final String JAVA_EXTENSION = ".java";

    static {
        cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setTemplateLoader(new ClassTemplateLoader(SetupTemplates.class, "/"));
    }

    public void createNewProjectPomFile(Map<String, String> context, File pomFile) throws MojoExecutionException {
        try {
            Template temp = cfg.getTemplate("templates/pom-template.ftl");
            Writer out = new FileWriter(pomFile);
            temp.process(context, out);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to generate pom.xml", e);
        }
    }

    public void generate(MavenProject project, Model model, String rootPath, String path, String className, Log log) throws MojoExecutionException {
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
        context.put("classname", className);
        context.put("root_prefix", rootPath);
        context.put("path", path);
        if (packageName != null) {
            context.put("packageName", packageName);
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

        // Generate application.
        File appClassFile = new File(root, "ShamrockApplication.java");
        try {
            Template temp = cfg.getTemplate("templates/application-template.ftl");
            Writer out = new FileWriter(appClassFile);
            temp.process(context, out);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to generate Application class", e);
        }


    }

    public void createIndexPage(Map<String, String> context, File basedir, Log log) throws MojoExecutionException {
        // Generate index page
        File resources = new File(basedir, "src/main/resources/META-INF/resources");
        File index = new File(mkdirs(resources, log), "index.html");
        if (!index.exists()) {
            try (Writer out = new FileWriter(index)) {
                Template temp = cfg.getTemplate("templates/index.ftl");
                temp.process(context, out);
                log.info("Welcome page created in src/main/resources/META-INF/resources/" + index.getName());
            } catch (Exception e) {
                throw new MojoExecutionException("Unable to generate the welcome page", e);
            }
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
