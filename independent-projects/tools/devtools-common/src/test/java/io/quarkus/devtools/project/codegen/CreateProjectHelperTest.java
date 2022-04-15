package io.quarkus.devtools.project.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateProjectHelperTest {

    @Test
    public void givenJavaVersion17ShouldReturn17() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonnull", "nonnull");

        CreateProjectHelper.setJavaVersion(values, "17");
        assertEquals("17", values.get("java_target"));
    }

    @Test
    public void givenJavaVersion16ShouldReturn11() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonnull", "nonnull");

        CreateProjectHelper.setJavaVersion(values, "16.0.1");
        assertEquals("11", values.get("java_target"));
    }

    @Test
    public void givenJavaVersion11ShouldReturn11() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonnull", "nonnull");

        CreateProjectHelper.setJavaVersion(values, "11");
        assertEquals("11", values.get("java_target"));
    }

    @Test
    public void givenJavaVersion18ShouldReturn17() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonnull", "nonnull");

        CreateProjectHelper.setJavaVersion(values, "18");
        assertEquals("17", values.get("java_target"));
    }

    @Test
    public void givenAutoDetectShouldReturnAppropriateVersion() {
        Map<String, Object> values = new HashMap<>();
        values.put("nonnull", "nonnull");

        CreateProjectHelper.setJavaVersion(values, CreateProjectHelper.DETECT_JAVA_RUNTIME_VERSION);
        assertEquals(String.valueOf(CreateProjectHelper.determineBestJavaLtsVersion(Runtime.version().feature())),
                values.get("java_target"));
    }

    @Test
    public void testDetermineBestLtsVersion() {
        assertEquals(11, CreateProjectHelper.determineBestJavaLtsVersion(8));
        assertEquals(11, CreateProjectHelper.determineBestJavaLtsVersion(11));
        assertEquals(11, CreateProjectHelper.determineBestJavaLtsVersion(12));
        assertEquals(17, CreateProjectHelper.determineBestJavaLtsVersion(17));
        assertEquals(17, CreateProjectHelper.determineBestJavaLtsVersion(18));
    }

    @Test
    public void givenKotlinProjectWithVersion18ShouldReturn17() {
        Map<String, Object> values = new HashMap<>();
        values.put(ProjectGenerator.SOURCE_TYPE, SourceType.KOTLIN);

        CreateProjectHelper.setJavaVersion(values, "18");
        assertEquals("17", values.get("java_target"));
    }
}
