package io.quarkus.it.hibernate.orm.rest.data.panache.repository;

import io.quarkus.it.hibernate.orm.rest.data.panache.common.AbstractAuthorResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthorPojoResourceTest extends AbstractAuthorResourceTest {

    @Override
    protected String getResourceName() {
        return "author-pojo";
    }
}
