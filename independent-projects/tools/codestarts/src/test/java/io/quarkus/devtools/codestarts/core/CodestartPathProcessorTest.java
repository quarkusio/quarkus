package io.quarkus.devtools.codestarts.core;

import static io.quarkus.devtools.codestarts.utils.NestedMaps.unflatten;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.quarkus.devtools.codestarts.CodestartStructureException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CodestartPathProcessorTest {

    @Test
    void testProcess() {
        Map<String, Object> dataFlat = new HashMap<>();
        dataFlat.put("foo.bar", "vfoobar");
        dataFlat.put("baz", "vbaz");
        dataFlat.put("package-name", "org.acme.andy");
        dataFlat.put("class-name", "MyResource");
        final Map<String, Object> data = unflatten(dataFlat);
        assertThat(CodestartPathProcessor.process("/foo/bar/{foo.bar}/baz", data))
                .isEqualTo("/foo/bar/vfoobar/baz");
        assertThat(CodestartPathProcessor.process("{foo.bar}", data))
                .isEqualTo("vfoobar");
        assertThat(CodestartPathProcessor.process("{foo.bar}/{foo.bar}", data))
                .isEqualTo("vfoobar/vfoobar");
        assertThat(CodestartPathProcessor.process("{foo.bar}/{baz}", data))
                .isEqualTo("vfoobar/vbaz");
        assertThat(CodestartPathProcessor.process("{foo.bar}/{baz}.txt", data))
                .isEqualTo("vfoobar/vbaz.txt");
        assertThat(CodestartPathProcessor.process("src/main/java/{package-name.dir}/{class-name}.java", data))
                .isEqualTo("src/main/java/org/acme/andy/MyResource.java");
        assertThat(CodestartPathProcessor.process("src/main/java/{package-name}/{class-name}.java", data))
                .isEqualTo("src/main/java/org.acme.andy/MyResource.java");
        assertThat(CodestartPathProcessor.process("foo/bar.txt", data))
                .isEqualTo("foo/bar.txt");
        assertThatExceptionOfType(CodestartStructureException.class)
                .isThrownBy(() -> CodestartPathProcessor.process("{foo.bar}/{baz}/{foo.baz}.txt", data))
                .withMessageContaining("{foo.baz}");
    }
}
