package io.quarkus.it.panache;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TransactionalRepositoryTest {

    @Inject
    BeerRepository beerRepository;

    @Test
    public void testTransactionalRepository() {
        // Make sure there are no beers stored
        beerRepository.deleteAll();

        Beer b = new Beer();
        b.name = "IPA";
        beerRepository.persist(b);

        Assertions.assertEquals(1, beerRepository.count());
    }

}
