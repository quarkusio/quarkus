package io.quarkus.it.rest.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.main.ClientWithExceptionMapperAndInterceptor;
import io.quarkus.it.rest.client.main.MyResponseExceptionMapper;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CheckedExceptionWithInterceptionTest {
    @Inject
    @RestClient
    ClientWithExceptionMapperAndInterceptor client;

    @Test
    public void test() {
        assertThrows(MyResponseExceptionMapper.MyException.class, () -> {
            client.get();
        });
    }
}
