package io.quarkus.it.hibernate.orm.rest.data.panache.entity;

import io.quarkus.it.hibernate.orm.rest.data.panache.common.AbstractBookResourceTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BookEntityResourceTest extends AbstractBookResourceTest {

    @Override
    protected String getResourceName() {
        return "book-entity";
    }
}
