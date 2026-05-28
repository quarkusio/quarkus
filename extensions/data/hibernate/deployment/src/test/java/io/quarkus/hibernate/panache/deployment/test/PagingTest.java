package io.quarkus.hibernate.panache.deployment.test;

import java.util.List;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;
import io.quarkus.test.QuarkusExtensionTest;

public class PagingTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyEntity.class, MyEntity_.class, MyEntity_.ManagedBlockingQueries_.class,
                            MyEntity_.FindOnlyRepo_.class));

    @Transactional
    void offsetPage() {
        PanacheBlockingQuery<MyEntity> query = MyEntity_.managedBlocking().findAll();

        List<MyEntity> list = query.paging().offset(0, 10).list();
        while (query.paging().hasNext()) {
            list = query.paging().next().list();
        }
    }

    @Transactional
    void cursorPage() {
        PanacheBlockingQuery<MyEntity> query = MyEntity_.managedBlocking().findAll(Order.by(Sort.asc("foo")));

        List<MyEntity> list = query.paging().cursored(0, 10).list();
        while (query.paging().hasNext()) {
            list = query.paging().next().list();
        }
    }

    @Transactional
    void limitPage() {
        PanacheBlockingQuery<MyEntity> query = MyEntity_.managedBlocking().findAll();

        List<MyEntity> list = query.limiting().limit(10).list();
    }

    @Test
    void testRepositories() {
    }

}
