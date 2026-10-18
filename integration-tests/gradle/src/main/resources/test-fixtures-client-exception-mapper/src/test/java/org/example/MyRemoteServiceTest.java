package org.example;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MyRemoteServiceTest {

    @Inject
    @RestClient
    MyRemoteService myRemoteService;

    @Test
    void testGetExtensionsById() {
        Set<MyRemoteService.Extension> extensions = 
            myRemoteService.getExtensionsById("io.quarkus:quarkus-hibernate-validator");
        
        assertNotNull(extensions);
    }
}