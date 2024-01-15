package io.quarkus.it.hibernate.search.orm.elasticsearch.management;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class HibernateSearchManagementIT extends HibernateSearchManagementTest {
    @Override
    protected String getPrefix() {
        return "http://localhost:9000"; // ITs run in prod mode.
    }
}
