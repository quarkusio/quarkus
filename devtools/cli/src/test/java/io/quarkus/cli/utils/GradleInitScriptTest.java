package io.quarkus.cli.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class GradleInitScriptTest {

    @Test
    public void testGetInitScript() {
        assertTrue(GradleInitScript.getInitScript(List.of("arg1", "arg2", "arg3")).isEmpty());
        assertEquals("/some/path",
                GradleInitScript.getInitScript(List.of("arg1", "arg2", "--init-script=/some/path")).get());
        assertEquals("/some/path",
                GradleInitScript.getInitScript(List.of("arg1", "--init-script=/some/path", "arg3")).get());
        assertEquals("/some/path",
                GradleInitScript.getInitScript(List.of("--init-script=/some/path", "arg2", "arg3")).get());
    }

    @Test
    public void testReadInitScriptDependencies() throws IOException {
        Path path = GradleInitScript.createInitScript(Set.of("g1:a1:v1", "g2:a2:v2"));
        System.out.println("path:" + path.toAbsolutePath().toString());
        System.out.println("content:" + Files.readString(path));
        List<String> gavs = GradleInitScript.readInitScriptDependencies(path);
        System.out.println("deps:" + gavs.stream().collect(Collectors.joining(", ", "[", "]")));
        assertTrue(gavs.contains("g1:a1:v1"));
        assertTrue(gavs.contains("g2:a2:v2"));
    }

    @Test
    public void shouldGenerateSignleInitScriptParam() throws IOException {
        List<String> params = new ArrayList<>();
        GradleInitScript.populateForExtensions(List.of("g1:a1"), params);
        GradleInitScript.populateForExtensions(List.of("g2:a2"), params);
        assertEquals(1l, params.stream().filter(p -> p.contains("--init-script=")).count());
        Optional<Path> path = GradleInitScript.getInitScript(params).map(Path::of);
        List<String> gavs = GradleInitScript.readInitScriptDependencies(path.get());
        System.out.println("gavs:" + gavs.stream().collect(Collectors.joining(",", "[", "]")));
        assertTrue(gavs.contains("g1:a1:${quarkusPlatformVersion}"));
        assertTrue(gavs.contains("g2:a2:${quarkusPlatformVersion}"));
    }
}
