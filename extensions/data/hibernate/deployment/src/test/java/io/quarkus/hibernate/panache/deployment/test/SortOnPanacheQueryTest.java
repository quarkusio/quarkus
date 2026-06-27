package io.quarkus.hibernate.panache.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;
import io.quarkus.test.QuarkusExtensionTest;

public class SortOnPanacheQueryTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyEntity.class, MyEntity_.class, _MyEntity.class,
                            MyEntity_.ManagedBlockingQueries_.class));

    @Transactional
    void createEntities() {
        MyEntity_.managedBlocking().deleteAll();
        for (int i = 0; i < 5; i++) {
            MyEntity entity = new MyEntity();
            entity.foo = "foo" + i;
            entity.bar = "bar" + (4 - i);
            entity.persist();
        }
    }

    @Transactional
    void findAllWithSort() {
        List<MyEntity> sorted = MyEntity_.managedBlocking().findAll()
                .sort(Sort.asc("foo"))
                .list();
        assertThat(sorted).hasSize(5);
        assertThat(sorted.get(0).foo).isEqualTo("foo0");
        assertThat(sorted.get(4).foo).isEqualTo("foo4");
    }

    @Transactional
    void findAllWithSortDesc() {
        List<MyEntity> sorted = MyEntity_.managedBlocking().findAll()
                .sort(Sort.desc("foo"))
                .list();
        assertThat(sorted.get(0).foo).isEqualTo("foo4");
        assertThat(sorted.get(4).foo).isEqualTo("foo0");
    }

    @Transactional
    void findWithSort() {
        List<MyEntity> sorted = MyEntity_.managedBlocking().find("foo", "foo2")
                .sort(Sort.asc("bar"))
                .list();
        assertThat(sorted).hasSize(1);
        assertThat(sorted.get(0).foo).isEqualTo("foo2");
    }

    @Transactional
    void findAllWithNullSort() {
        List<MyEntity> unsorted = MyEntity_.managedBlocking().findAll()
                .sort((Order<? super MyEntity>) null)
                .list();
        assertThat(unsorted).hasSize(5);
    }

    @Transactional
    void findAllWithSortShortcut() {
        List<MyEntity> sorted = MyEntity_.managedBlocking().findAll()
                .sort(Sort.asc("foo"))
                .list();
        assertThat(sorted.get(0).foo).isEqualTo("foo0");
    }

    @Transactional
    void findAllWithEmptyOrder() {
        List<MyEntity> unsorted = MyEntity_.managedBlocking().findAll()
                .sort(Order.by())
                .list();
        assertThat(unsorted).hasSize(5);
    }

    @Transactional
    void findAllWithNullSortShortcut() {
        List<MyEntity> unsorted = MyEntity_.managedBlocking().findAll()
                .sort((Sort<? super MyEntity>) null)
                .list();
        assertThat(unsorted).hasSize(5);
    }

    @Transactional
    void streamWithSort() {
        List<String> names = MyEntity_.managedBlocking().findAll()
                .sort(Sort.asc("foo"))
                .stream()
                .map(e -> e.foo)
                .collect(Collectors.toList());
        assertThat(names).containsExactly("foo0", "foo1", "foo2", "foo3", "foo4");
    }

    @Transactional
    void replaceSort() {
        PanacheBlockingQuery<MyEntity> query = MyEntity_.managedBlocking().findAll()
                .sort(Sort.asc("foo"));
        assertThat(query.list().get(0).foo).isEqualTo("foo0");

        List<MyEntity> resorted = query.sort(Sort.desc("foo")).list();
        assertThat(resorted.get(0).foo).isEqualTo("foo4");
    }

    @Transactional
    void clear() {
        MyEntity_.managedBlocking().deleteAll();
    }

    @Test
    void testSortOnPanacheQuery() {
        createEntities();
        findAllWithSort();
        findAllWithSortDesc();
        findWithSort();
        findAllWithNullSort();
        findAllWithEmptyOrder();
        findAllWithSortShortcut();
        findAllWithNullSortShortcut();
        streamWithSort();
        replaceSort();
        clear();
    }
}
