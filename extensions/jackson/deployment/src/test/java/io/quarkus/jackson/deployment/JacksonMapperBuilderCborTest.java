package io.quarkus.jackson.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonMapperBuilderCborTest extends JacksonMapperBuilderTypeTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.jackson.mapper-builder-type", "cbor");

    @Override
    ObjectMapper explicitObjectMapper() {
        return CBORMapper.builder().build();
    }
}
