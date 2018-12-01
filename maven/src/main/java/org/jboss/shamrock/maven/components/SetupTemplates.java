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

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
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

    public void generate(MavenProject project, String rootPath, String path, String className, Log log) throws MojoExecutionException {
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
            if (!packageDir.exists()) {
                packageDir.mkdirs();
                log.info("Creating directory " + packageDir.getAbsolutePath());
            }
            root = packageDir;

            File testPackageDir = new File(testRoot, packageName.replace('.', '/'));
            if (!testPackageDir.exists()) {
                testPackageDir.mkdirs();
                log.info("Creating directory " + packageDir.getAbsolutePath());
            }
            testRoot = testPackageDir;
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

}
