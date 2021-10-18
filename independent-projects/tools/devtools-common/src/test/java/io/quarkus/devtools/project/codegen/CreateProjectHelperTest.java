package io.quarkus.devtools.project.codegen;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class CreateProjectHelperTest {

    @Test
    public void givenJavaVersion17ShouldReturn17() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonull", "nonull");
        CreateProjectHelper.setJavaVersion(values, null);

        values.entrySet().forEach(System.out::println);
    }
}