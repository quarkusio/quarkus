package io.quarkus.jackson.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonMapperBuilderJsonTest extends JacksonMapperBuilderTypeTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.jackson.mapper-builder-type", "json");

    @Override
    ObjectMapper explicitObjectMapper() {
        return JsonMapper.builder().build();
    }
}
