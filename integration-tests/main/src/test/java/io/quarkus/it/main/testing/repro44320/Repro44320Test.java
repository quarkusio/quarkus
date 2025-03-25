package io.quarkus.it.main.testing.repro44320;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.it.testing.repro44320.MyService;
import io.quarkus.test.junit.QuarkusTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class Repro44320Test {
    private static Set<String> set = new HashSet<>();

    @Inject
    MyService service;

    @BeforeAll
    public void beforeAllTests() {
        set = service.get();
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void test(String key) {
        assertNotNull(key);
    }

    public Set<String> getData() {
        return set;
    }
}
