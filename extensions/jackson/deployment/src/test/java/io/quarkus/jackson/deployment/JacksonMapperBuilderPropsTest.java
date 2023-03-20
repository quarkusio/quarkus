package io.quarkus.jackson.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonMapperBuilderPropsTest extends JacksonMapperBuilderTypeTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.jackson.mapper-builder-type", "javaprops");

    @Override
    ObjectMapper explicitObjectMapper() {
        return JavaPropsMapper.builder().build();
    }
}
