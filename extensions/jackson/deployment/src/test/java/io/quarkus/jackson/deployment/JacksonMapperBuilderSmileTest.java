package io.quarkus.jackson.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonMapperBuilderSmileTest extends JacksonMapperBuilderTypeTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.jackson.mapper-builder-type", "smile");

    @Override
    ObjectMapper explicitObjectMapper() {
        return SmileMapper.builder().build();
    }
}
