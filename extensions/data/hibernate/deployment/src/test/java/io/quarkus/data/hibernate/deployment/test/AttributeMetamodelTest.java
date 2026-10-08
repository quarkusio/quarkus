package io.quarkus.data.hibernate.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.data.hibernate.blocking.BlockingDataQuery;
import io.quarkus.test.QuarkusExtensionTest;

public class AttributeMetamodelTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyEntity.class, MyEntity_.class, _MyEntity.class,
                            _MyEntity._ManagedBlockingQueries.class,
                            _MyEntity._FindOnlyRepo.class));

    @Transactional
    void setup() {
        MyEntity_.managedBlocking().deleteAll();

        MyEntity entity1 = new MyEntity();
        entity1.foo = "alpha";
        entity1.bar = "one";
        entity1.persist();

        MyEntity entity2 = new MyEntity();
        entity2.foo = "beta";
        entity2.bar = "two";
        entity2.persist();

        MyEntity entity3 = new MyEntity();
        entity3.foo = "alpha";
        entity3.bar = "three";
        entity3.persist();

        MyEntity entity4 = new MyEntity();
        entity4.foo = null;
        entity4.bar = "four";
        entity4.persist();
    }

    @Transactional
    void testFindWithJakartaDataAttribute() {
        BlockingDataQuery<MyEntity> query = MyEntity_.managedBlocking().find(_MyEntity.foo, "alpha");
        List<MyEntity> results = query.list();
        assertThat(results).hasSize(2);
        assertThat(results).extracting(e -> e.foo).containsOnly("alpha");
    }

    @Transactional
    void testFindWithJpaAttributeStillWorks() {
        BlockingDataQuery<MyEntity> query = MyEntity_.managedBlocking().find(MyEntity_.FOO, "alpha");
        List<MyEntity> results = query.list();
        assertThat(results).hasSize(2);
        assertThat(results).extracting(e -> e.foo).containsOnly("alpha");
    }

    @Transactional
    void testListWithJakartaDataAttribute() {
        List<MyEntity> results = MyEntity_.managedBlocking().list(_MyEntity.foo, "beta");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).foo).isEqualTo("beta");
        assertThat(results.get(0).bar).isEqualTo("two");
    }

    @Transactional
    void testListWithJpaAttributeStillWorks() {
        List<MyEntity> results = MyEntity_.managedBlocking().list(MyEntity_.FOO, "beta");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).foo).isEqualTo("beta");
    }

    @Transactional
    void testStreamWithJakartaDataAttribute() {
        List<MyEntity> results = MyEntity_.managedBlocking().stream(_MyEntity.foo, "alpha")
                .collect(Collectors.toList());
        assertThat(results).hasSize(2);
        assertThat(results).extracting(e -> e.foo).containsOnly("alpha");
    }

    @Transactional
    void testCountWithJakartaDataAttribute() {
        Long count = MyEntity_.managedBlocking().count(_MyEntity.foo, "alpha");
        assertThat(count).isEqualTo(2L);
    }

    @Transactional
    void testCountWithJpaAttributeStillWorks() {
        Long count = MyEntity_.managedBlocking().count(MyEntity_.FOO, "alpha");
        assertThat(count).isEqualTo(2L);
    }

    @Transactional
    void testCountNoResults() {
        Long count = MyEntity_.managedBlocking().count(_MyEntity.foo, "nonexistent");
        assertThat(count).isEqualTo(0L);
    }

    @Transactional
    void testDeleteWithJakartaDataAttribute() {
        long deleted = MyEntity_.managedBlocking().delete(_MyEntity.foo, "beta");
        assertThat(deleted).isEqualTo(1L);
        assertThat(MyEntity_.managedBlocking().count()).isEqualTo(3L);
    }

    @Transactional
    void testDeleteWithJpaAttributeStillWorks() {
        long deleted = MyEntity_.managedBlocking().delete(MyEntity_.FOO, "alpha");
        assertThat(deleted).isEqualTo(2L);
        assertThat(MyEntity_.managedBlocking().count()).isEqualTo(2L);
    }

    @Transactional
    void testDeleteNoMatches() {
        long deleted = MyEntity_.managedBlocking().delete(_MyEntity.foo, "nonexistent");
        assertThat(deleted).isEqualTo(0L);
        assertThat(MyEntity_.managedBlocking().count()).isEqualTo(4L);
    }

    @Transactional
    void testFindWithNullValue() {
        List<MyEntity> results = MyEntity_.managedBlocking().list(_MyEntity.foo, null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).bar).isEqualTo("four");
    }

    @Transactional
    void testMultipleOperationsOnSameAttribute() {
        Long initialCount = MyEntity_.managedBlocking().count(_MyEntity.foo, "alpha");
        assertThat(initialCount).isEqualTo(2L);

        List<MyEntity> results = MyEntity_.managedBlocking().list(_MyEntity.foo, "alpha");
        assertThat(results).hasSize(2);

        long deleted = MyEntity_.managedBlocking().delete(_MyEntity.bar, "one");
        assertThat(deleted).isEqualTo(1L);

        Long finalCount = MyEntity_.managedBlocking().count(_MyEntity.foo, "alpha");
        assertThat(finalCount).isEqualTo(1L);
    }

    @Transactional
    void cleanup() {
        MyEntity_.managedBlocking().deleteAll();
    }

    @Test
    void testJakartaDataMetamodelSupport() {
        setup();
        testFindWithJakartaDataAttribute();
        testFindWithJpaAttributeStillWorks();
        testListWithJakartaDataAttribute();
        testListWithJpaAttributeStillWorks();
        testStreamWithJakartaDataAttribute();
        testCountWithJakartaDataAttribute();
        testCountWithJpaAttributeStillWorks();
        testCountNoResults();
        testDeleteWithJakartaDataAttribute();
        cleanup();

        setup();
        testDeleteWithJpaAttributeStillWorks();
        cleanup();

        setup();
        testDeleteNoMatches();
        testFindWithNullValue();
        testMultipleOperationsOnSameAttribute();
        cleanup();
    }
}
