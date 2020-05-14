package io.quarkus.it.panache.rest.entity;

import io.quarkus.it.panache.rest.common.AbstractAuthorCrudResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthorEntityResourceTest extends AbstractAuthorCrudResourceTest {

    @Override
    protected String getResourceName() {
        return "author-entity";
    }
}
