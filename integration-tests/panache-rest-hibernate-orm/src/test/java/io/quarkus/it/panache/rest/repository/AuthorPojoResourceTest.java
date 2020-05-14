package io.quarkus.it.panache.rest.repository;

import io.quarkus.it.panache.rest.common.AbstractAuthorCrudResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthorPojoResourceTest extends AbstractAuthorCrudResourceTest {

    @Override
    protected String getResourceName() {
        return "author-pojo";
    }
}
