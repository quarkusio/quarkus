/*
 *  Copyright (c) 2019 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.utils.StringUtils;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

public class ExtensionListIT {

    private RunningInvoker running;
    private File testDir;

    @Test
    public void testExtensionListCreation() throws MavenInvocationException, IOException, InterruptedException {
        testDir = initProject();

        running = new RunningInvoker(testDir, false);
        final MavenProcessInvocationResult result = running.execute(Collections.singletonList("package"),
                Collections.emptyMap());

        assertThat(result.getProcess().waitFor()).isEqualTo(0);

        File jsonFile = new File(new File(testDir, "extension-list/target/classes"), "extension-list.json");
        assertThat(jsonFile).isFile();

        String data = FileUtils.readFileToString(jsonFile, "UTF-8");
        assertThat(data).contains("\"artifactId\" : \"dummy-extension-deployment\",");
        assertThat(data).contains("\"name\" : \"Agroal\",");
        assertThat(data).contains("\"description\" : \"Description of the Agroal extension\",");
        assertThat(data).contains("\"labels\" : [ \"agroal\", \"database-connection-pool\", \"jsf\" ]");
        assertThat(data).contains("\"internal\" : false,");
    }

    static File initProject() {
        String name = "projects/extension-check";
        String output = "projects/project-extension-check";

        File tc = new File("target/test-classes");
        if (!tc.isDirectory()) {
            boolean mkdirs = tc.mkdirs();
            Logger.getLogger(ExtensionListIT.class.getName())
                    .log(Level.FINE, "test-classes created? " + mkdirs);
        }

        File in = new File("src/test/resources", name);
        if (!in.isDirectory()) {
            throw new RuntimeException("Cannot find directory: " + in.getAbsolutePath());
        }

        File out = new File(tc, output);
        if (out.isDirectory()) {
            FileUtils.deleteQuietly(out);
        }
        boolean mkdirs = out.mkdirs();
        Logger.getLogger(ExtensionListIT.class.getName())
                .log(Level.FINE, out.getAbsolutePath() + " created? " + mkdirs);
        try {
            org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(in, out);
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy project resources", e);
        }

        File extMod = new File(out, "extension-list");
        if (!extMod.isDirectory()) {
            boolean dirs = extMod.mkdirs();
            Logger.getLogger(ExtensionListIT.class.getName())
                    .log(Level.FINE, "extension-list created? " + dirs);
        }

        File pom = new File(extMod, "pom.xml");
        try {
            filter(pom, ImmutableMap.of("@project.version@", System.getProperty("project.version")));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return out;
    }

    static void filter(File input, Map<String, String> variables) throws IOException {
        assertThat(input).isFile();
        String data = FileUtils.readFileToString(input, "UTF-8");

        for (Map.Entry<String, String> token : variables.entrySet()) {
            String value = String.valueOf(token.getValue());
            data = StringUtils.replace(data, token.getKey(), value);
        }
        FileUtils.write(input, data, "UTF-8");
    }

    public static void installPluginToLocalRepository(File local) {
        File repo = new File(local, "io.quarkus/quarkus-extension-plugin/" + System.getProperty("project.version"));
        if (!repo.isDirectory()) {
            boolean mkdirs = repo.mkdirs();
            Logger.getLogger(ExtensionListIT.class.getName())
                    .log(Level.FINE, repo.getAbsolutePath() + " created? " + mkdirs);
        }

        File plugin = new File("target", "quarkus-extension-plugin-" + System.getProperty("project.version") + ".jar");
        if (!plugin.isFile()) {
            File[] files = new File("target").listFiles(
                    file -> file.getName().startsWith("quarkus-extension-plugin") && file.getName().endsWith(".jar"));
            if (files != null && files.length != 0) {
                plugin = files[0];
            }
        }

        try {
            FileUtils.copyFileToDirectory(plugin, repo);
            String installedPomName = "quarkus-extension-plugin-" + System.getProperty("project.version") + ".pom";
            FileUtils.copyFile(new File("pom.xml"), new File(repo, installedPomName));
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy the plugin jar, or the pom file, to the local repository", e);
        }
    }
}
