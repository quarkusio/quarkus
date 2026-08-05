package io.quarkus.jackson.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.ObjectMapper;

public class JacksonUseGettersAsSettersDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest();

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testGettersNotUsedAsSettersByDefault() {
        BeanWithListGetter result = objectMapper.readValue("{\"items\":[\"a\",\"b\"]}", BeanWithListGetter.class);
        assertThat(result.getItems()).isEmpty();
    }

    public static class BeanWithListGetter {
        private final List<String> items = new ArrayList<>();

        public List<String> getItems() {
            return items;
        }
    }
}
