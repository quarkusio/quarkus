package io.quarkus.it.panache.defaultpu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TransactionalPanacheEntityTest {

    @Test
    public void testTransactionalPanacheEntity() {
        Beer b = new Beer();
        b.name = "IPA";
        // Method is annotated with @Transactional and should be intercepted
        Beer.deleteAllAndPersist(b);
        assertEquals(1, Beer.count());
    }

}
