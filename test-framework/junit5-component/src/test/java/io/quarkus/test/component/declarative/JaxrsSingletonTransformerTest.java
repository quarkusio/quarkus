package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.beans.Charlie;

@QuarkusComponentTest
public class JaxrsSingletonTransformerTest {

    @Inject
    MyResource resource;

    @InjectMock
    Charlie charlie;

    @Test
    public void testPing() {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo", resource.ping());
    }

    // @Singleton should be added automatically
    @Path("my")
    public static class MyResource {

        @Inject
        Charlie charlie;

        @GET
        public String ping() {
            return charlie.ping();
        }

    }

}
