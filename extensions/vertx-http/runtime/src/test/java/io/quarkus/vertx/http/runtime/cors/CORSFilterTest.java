package io.quarkus.vertx.http.runtime.cors;

import static io.quarkus.vertx.http.runtime.cors.CORSFilter.isConfiguredWithWildcard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CORSFilterTest {

    @Test
    public void isConfiguredWithWildcardTest() {
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.empty()));
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.of(Collections.EMPTY_LIST)));
        Assertions.assertTrue(isConfiguredWithWildcard(Optional.of(Collections.singletonList("*"))));

        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Arrays.asList("PUT", "GET", "POST"))));
        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Arrays.asList("http://localhost:8080/", "*"))));
        Assertions.assertFalse(isConfiguredWithWildcard(Optional.of(Collections.singletonList("http://localhost:8080/"))));
    }

}
