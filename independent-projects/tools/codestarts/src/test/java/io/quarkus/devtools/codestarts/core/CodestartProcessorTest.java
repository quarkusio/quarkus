package io.quarkus.devtools.codestarts.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.codestarts.core.strategy.CodestartFileStrategyHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;

class CodestartProcessorTest {

    @Test
    void checkSelectedDefaultStrategy() {
        Map<String, String> spec = new HashMap<>();
        spec.put("test/foo.tt", "forbidden");
        spec.put("*", "replace");
        spec.put("docker-compose-include-test.yml", "docker-compose-includes");

        final CodestartProcessor processor = new CodestartProcessor(
                MessageWriter.info(),
                "a",
                Paths.get("test"),
                CodestartProcessor.buildStrategies(spec),
                Collections.emptyMap());

        assertThat(processor.getSelectedDefaultStrategy()).isEqualTo(CodestartFileStrategyHandler.BY_NAME.get("replace"));
        assertThat(processor.getStrategy("test/foo.tt")).hasValue(CodestartFileStrategyHandler.BY_NAME.get("forbidden"));
        assertThat(processor.getStrategy("docker-compose-include-test.yml"))
                .hasValue(CodestartFileStrategyHandler.BY_NAME.get("docker-compose-includes"));
    }

    @Test
    void checkDefaultStrategy() {
        Map<String, String> spec = new HashMap<>();
        spec.put("test/foo.tt", "forbidden");

        final CodestartProcessor processor = new CodestartProcessor(
                MessageWriter.info(),
                "a",
                Paths.get("test"),
                CodestartProcessor.buildStrategies(spec),
                Collections.emptyMap());

        assertThat(processor.getSelectedDefaultStrategy())
                .isEqualTo(CodestartFileStrategyHandler.BY_NAME.get("fail-on-duplicate"));
        assertThat(processor.getStrategy("test/foo.tt")).hasValue(CodestartFileStrategyHandler.BY_NAME.get("forbidden"));
    }
}
