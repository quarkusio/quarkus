package io.quarkus.it.panache.rest.entity;

import io.quarkus.it.panache.rest.common.AbstractBookCrudResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BookEntityCrudResourceTest extends AbstractBookCrudResourceTest {

    @Override
    protected String getResourceName() {
        return "book-entity";
    }
}
