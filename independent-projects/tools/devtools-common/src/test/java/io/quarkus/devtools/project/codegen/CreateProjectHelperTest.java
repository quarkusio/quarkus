package io.quarkus.devtools.project.codegen;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateProjectHelperTest {

    @Test
    public void givenJavaVersion17ShouldReturn17() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonull", "nonull");

        CreateProjectHelper.setJavaVersion(values, "17");
        assertEquals("17", values.get("java_target"));
    }

    @Test
    public void givenJavaVersion16ShouldReturn11() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonull", "nonull");

        CreateProjectHelper.setJavaVersion(values, "16.0.1");
        assertEquals("11", values.get("java_target"));
    }
}
