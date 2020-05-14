package io.quarkus.it.panache.rest.repository;

import io.quarkus.it.panache.rest.common.AbstractBookCrudResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BookPojoRepositoryCrudResourceTest extends AbstractBookCrudResourceTest {

    @Override
    protected String getResourceName() {
        return "book-pojo";
    }
}
