package io.quarkus.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.utils.SkillComposer;
import io.quarkus.devui.spi.buildtime.DevMcpBuildTimeTool;
import io.quarkus.devui.spi.buildtime.DevMcpParam;

public class AggregateSkillsMojoTest {

    @DevMcpBuildTimeTool(name = "simpleAction", description = "A simple action")
    static class SimpleProcessor {
    }

    @DevMcpBuildTimeTool(name = "actionWithParams", description = "Action with parameters", params = {
            @DevMcpParam(name = "className", description = "The test class name"),
            @DevMcpParam(name = "methodName", description = "The method name", required = false)
    })
    static class ProcessorWithParams {
    }

    @DevMcpBuildTimeTool(name = "firstAction", description = "First action")
    @DevMcpBuildTimeTool(name = "secondAction", description = "Second action")
    static class MultiToolProcessor {
    }

    static class NoAnnotationProcessor {
    }

    @Test
    public void scanSimpleAnnotation() throws IOException {
        Index index = indexClasses(SimpleProcessor.class);
        List<SkillComposer.McpToolInfo> tools = AggregateSkillsMojo.scanBuildTimeToolAnnotations(index);

        assertEquals(1, tools.size());
        assertEquals("simpleAction", tools.get(0).name());
        assertEquals("A simple action", tools.get(0).description());
        assertNull(tools.get(0).parameters());
    }

    @Test
    public void scanAnnotationWithParameters() throws IOException {
        Index index = indexClasses(ProcessorWithParams.class);
        List<SkillComposer.McpToolInfo> tools = AggregateSkillsMojo.scanBuildTimeToolAnnotations(index);

        assertEquals(1, tools.size());
        assertEquals("actionWithParams", tools.get(0).name());
        assertNotNull(tools.get(0).parameters());
        assertEquals(2, tools.get(0).parameters().size());
        assertEquals("The test class name", tools.get(0).parameters().get("className").description());
        assertTrue(tools.get(0).parameters().get("className").required());
        assertEquals("The method name", tools.get(0).parameters().get("methodName").description());
        assertFalse(tools.get(0).parameters().get("methodName").required());
    }

    @Test
    public void scanMultipleAnnotations() throws IOException {
        Index index = indexClasses(MultiToolProcessor.class);
        List<SkillComposer.McpToolInfo> tools = AggregateSkillsMojo.scanBuildTimeToolAnnotations(index);

        assertEquals(2, tools.size());
        assertEquals("firstAction", tools.get(0).name());
        assertEquals("First action", tools.get(0).description());
        assertEquals("secondAction", tools.get(1).name());
        assertEquals("Second action", tools.get(1).description());
    }

    @Test
    public void scanClassWithoutAnnotation() throws IOException {
        Index index = indexClasses(NoAnnotationProcessor.class);
        List<SkillComposer.McpToolInfo> tools = AggregateSkillsMojo.scanBuildTimeToolAnnotations(index);

        assertTrue(tools.isEmpty());
    }

    private static Index indexClasses(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            String classFile = clazz.getName().replace('.', '/') + ".class";
            try (var is = clazz.getClassLoader().getResourceAsStream(classFile)) {
                indexer.index(is);
            }
        }
        return indexer.complete();
    }
}
