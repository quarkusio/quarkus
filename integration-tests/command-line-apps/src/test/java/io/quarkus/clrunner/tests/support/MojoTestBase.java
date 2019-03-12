package io.quarkus.clrunner.tests.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.utils.StringUtils;
import org.junit.jupiter.api.BeforeAll;

import com.google.common.collect.ImmutableMap;

import io.quarkus.maven.utilities.MojoUtils;

/*
 * Copied and adapted from devtools-maven
 */
public class MojoTestBase {
    private static ImmutableMap<String, String> VARIABLES;

    @BeforeAll
    public static void init() {
        VARIABLES = ImmutableMap.of(
                "@project.groupId@", MojoUtils.getPluginGroupId(),
                "@project.artifactId@", MojoUtils.getPluginArtifactId(),
                "@project.version@", MojoUtils.getPluginVersion());
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
            org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(in, out);
        } catch (IOException e) {
            throw new RuntimeException("Cannot copy project resources", e);
        }

        File pom = new File(out, "pom.xml");
        try {
            filter(pom, VARIABLES);
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

}
