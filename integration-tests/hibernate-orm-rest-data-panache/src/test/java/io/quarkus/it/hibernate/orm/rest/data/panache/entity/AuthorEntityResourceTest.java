package io.quarkus.it.hibernate.orm.rest.data.panache.entity;

import io.quarkus.it.hibernate.orm.rest.data.panache.common.AbstractAuthorResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthorEntityResourceTest extends AbstractAuthorResourceTest {

    @Override
    protected String getResourceName() {
        return "author-entity";
    }
}
