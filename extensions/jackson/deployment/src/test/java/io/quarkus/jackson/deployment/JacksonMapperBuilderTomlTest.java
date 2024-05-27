package io.quarkus.jackson.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonMapperBuilderTomlTest extends JacksonMapperBuilderTypeTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.jackson.mapper-builder-type", "toml");

    @Override
    ObjectMapper explicitObjectMapper() {
        return TomlMapper.builder().build();
    }
}
