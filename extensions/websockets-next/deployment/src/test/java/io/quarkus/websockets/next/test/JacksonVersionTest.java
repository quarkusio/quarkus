package io.quarkus.websockets.next.test;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class JacksonVersionTest {

    @Test
    public void jackson2NotOnClassPath() {
        Assertions.assertThatThrownBy(() -> Class.forName("com.fasterxml.jackson.databind.ObjectMapper", false,
                Thread.currentThread().getContextClassLoader()));
    }
}
