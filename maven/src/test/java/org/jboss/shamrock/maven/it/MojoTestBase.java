package org.jboss.shamrock.maven.it;


import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.utils.StringUtils;
import org.jboss.shamrock.maven.CreateProjectMojo;
import org.jboss.shamrock.maven.utilities.MojoUtils;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class MojoTestBase {
    static String VERSION;
    private static ImmutableMap<String, String> VARIABLES;

    @BeforeClass
    public static void init() {
        VERSION = MojoUtils.getVersion(CreateProjectMojo.VERSION_PROP);
        assertThat(VERSION).isNotNull();

        VARIABLES = ImmutableMap.of(
                "@project.groupId@", CreateProjectMojo.PLUGIN_GROUPID,
                "@project.artifactId@", CreateProjectMojo.PLUGIN_ARTIFACTID,
                "@project.version@", VERSION);
    }

    static File initProject(String name) {
        return initProject(name, name);
    }

    static File initEmptyProject(String name) {
        File tc = new File("target/test-classes/" + name);
        if (tc.isDirectory()) {
            boolean delete = tc.delete();
            Logger.getLogger(MojoTestBase.class.getName())
                    .log(Level.FINE, "test-classes deleted? " + delete);
        }
        boolean mkdirs = tc.mkdirs();
        Logger.getLogger(MojoTestBase.class.getName())
                .log(Level.FINE, "test-classes created? " + mkdirs);
        return tc;
    }

    public static File initProject(String name, String output) {
        File tc = new File("target/test-classes");
        if (!tc.isDirectory()) {
            boolean mkdirs = tc.mkdirs();
            Logger.getLogger(MojoTestBase.class.getName())
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
        Logger.getLogger(MojoTestBase.class.getName())
                .log(Level.FINE, out.getAbsolutePath() + " created? " + mkdirs);
        try {
            System.out.println("Copying " + in.getAbsolutePath() + " to " + out.getParentFile().getAbsolutePath());
            org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(in, out);
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy project resources", e);
        }
        return out;
    }

    static void installPluginToLocalRepository(File local) {
        File repo = new File(local, CreateProjectMojo.PLUGIN_GROUPID.replace(".", "/") + "/"
                + CreateProjectMojo.PLUGIN_ARTIFACTID + "/" + MojoTestBase.VERSION);
        if (!repo.isDirectory()) {
            boolean mkdirs = repo.mkdirs();
            Logger.getLogger(MojoTestBase.class.getName())
                    .log(Level.FINE, repo.getAbsolutePath() + " created? " + mkdirs);
        }

        File plugin = new File("target", CreateProjectMojo.PLUGIN_ARTIFACTID + "-" + MojoTestBase.VERSION + ".jar");
        if (! plugin.isFile()) {
            File[] files = new File("target").listFiles(
                    file -> file.getName().startsWith(CreateProjectMojo.PLUGIN_ARTIFACTID) && file.getName().endsWith(".jar"));
            if (files != null  && files.length != 0) {
                plugin = files[0];
            }
        }

        try {
            FileUtils.copyFileToDirectory(plugin, repo);
            String installedPomName = CreateProjectMojo.PLUGIN_ARTIFACTID + "-" + MojoTestBase.VERSION + ".pom";
            FileUtils.copyFile(new File("pom.xml"), new File(repo, installedPomName));
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy the plugin jar, or the pom file, to the local repository", e);
        }
    }

    static void installJarToLocalRepository(String local, String name, File jar) {
        File repo = new File(local, "org/acme/" + name + "/1.0");
        if (!repo.isDirectory()) {
            boolean mkdirs = repo.mkdirs();
            Logger.getLogger(MojoTestBase.class.getName())
                    .log(Level.FINE, repo.getAbsolutePath() + " created? " + mkdirs);
        }

        try {
            FileUtils.copyFileToDirectory(jar, repo);
            String installedPomName = name + "-1.0.pom";
            FileUtils.write(new File(repo, installedPomName), "<project>\n" +
                    "  <modelVersion>4.0.0</modelVersion>\n" +
                    "  <groupId>org.acme</groupId>\n" +
                    "  <artifactId>" + name + "</artifactId>\n" +
                    "  <version>1.0</version>\n" +
                    "</project>", "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy the jar, or the pom file, to the local repository", e);
        }
    }

    static void prepareProject(File testDir) throws IOException {
        File pom = new File(testDir, "pom.xml");
        assertThat(pom).isFile();
        filter(pom, VARIABLES);
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

    Map<String, String> getEnv() {
        String opts = System.getProperty("mavenOpts");
        Map<String, String> env = new HashMap<>();
        if (opts != null) {
            env.put("MAVEN_OPTS", opts);
        }
        return env;
    }
}
