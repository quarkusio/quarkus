package io.quarkus.it.hibernate.orm.rest.data.panache.repository;

import io.quarkus.it.hibernate.orm.rest.data.panache.common.AbstractBookResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BookPojoRepositoryResourceTest extends AbstractBookResourceTest {

    @Override
    protected String getResourceName() {
        return "book-pojo";
    }
}
