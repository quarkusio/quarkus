package io.quarkus.it.spring.data.jpa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

@QuarkusTest
public class PostRepositorySpyTest {

    // Without delegate = true, the call to the spy will fail with:
    // "Cannot call abstract real method on java object!"
    @InjectSpy(delegate = true)
    PostRepository repo;

    @Test
    void testDefaultMethodOfIntermediateRepositoryInSpy() {
        doReturn(new Post()).when(repo).findMandatoryById(1111L);
        assertNotNull(repo.findMandatoryById(1111L));
        verify(repo).findMandatoryById(1111L);
    }
}
