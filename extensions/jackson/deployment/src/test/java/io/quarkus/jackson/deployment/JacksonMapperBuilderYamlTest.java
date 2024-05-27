package io.quarkus.jackson.deployment;

import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.quarkus.test.QuarkusUnitTest;

public class JacksonMapperBuilderYamlTest extends JacksonMapperBuilderTypeTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.jackson.mapper-builder-type", "yaml");

    @Override
    ObjectMapper explicitObjectMapper() {
        return YAMLMapper.builder().build();
    }
}
