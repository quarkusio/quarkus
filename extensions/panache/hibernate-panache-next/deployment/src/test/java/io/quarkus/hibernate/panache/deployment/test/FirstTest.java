package io.quarkus.hibernate.panache.deployment.test;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FirstTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyEntity.class, MyEntity_.class, MyEntity_.ManagedBlockingQueries_.class));

    @Transactional
    void createOne() {
        Assertions.assertEquals(0, MyEntity_.managedBlocking().count());

        MyEntity entity = new MyEntity();
        entity.foo = "bar";
        entity.persist();

        Assertions.assertEquals(1, MyEntity_.managedBlocking().count());
    }

    @Transactional
    void modifyOne() {
        Assertions.assertEquals(1, MyEntity_.managedBlocking().count());

        MyEntity entity = MyEntity_.managedBlocking().listAll().get(0);
        Assertions.assertEquals("bar", entity.foo);
        entity.foo = "gee";

        Assertions.assertEquals(1, MyEntity_.managedBlocking().count());
    }

    @Transactional
    void modifyOneCheck() {
        Assertions.assertEquals(1, MyEntity_.managedBlocking().count());

        MyEntity entity = MyEntity_.managedBlocking().listAll().get(0);
        Assertions.assertEquals("gee", entity.foo);

        Assertions.assertEquals(1, MyEntity_.managedBlocking().count());
    }

    @Transactional
    void modifyOneStatelessNoUpdate() {
        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());

        MyEntity entity = MyEntity_.statelessBlocking().listAll().get(0);
        Assertions.assertEquals("gee", entity.foo);
        // should be ignored: not managed and no update called
        entity.foo = "fu";

        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());
    }

    @Transactional
    void modifyOneStateless() {
        MyEntity_.statelessBlocking().listAll();

        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());

        MyEntity entity = MyEntity_.statelessBlocking().listAll().get(0);
        // still the old value
        Assertions.assertEquals("gee", entity.foo);
        entity.foo = "fu";
        // make sure we call update
        entity.statelessBlocking().update();

        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());
    }

    @Transactional
    void modifyOneStatelessCheck() {
        Assertions.assertEquals(1, MyEntity_.managedBlocking().count());

        MyEntity entity = MyEntity_.managedBlocking().listAll().get(0);
        Assertions.assertEquals("fu", entity.foo);

        Assertions.assertEquals(1, MyEntity_.managedBlocking().count());
    }

    @Transactional
    void upsertNew() {
        Assertions.assertEquals(0, MyEntity_.statelessBlocking().count());

        MyEntity entity = new MyEntity();
        entity.foo = "bar";
        entity.id = 1L;
        entity.statelessBlocking().upsert();

        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());
    }

    @Transactional
    void upsertExisting() {
        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());

        MyEntity entity = MyEntity_.statelessBlocking().listAll().get(0);
        Assertions.assertEquals("bar", entity.foo);
        Assertions.assertEquals(1L, entity.id);
        entity.foo = "fu";
        entity.statelessBlocking().upsert();

        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());
    }

    @Transactional
    void upsertCheck() {
        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());

        MyEntity entity = MyEntity_.statelessBlocking().listAll().get(0);
        Assertions.assertEquals("fu", entity.foo);

        Assertions.assertEquals(1, MyEntity_.statelessBlocking().count());
    }

    @Transactional
    void clear() {
        MyEntity_.managedBlocking().deleteAll();
    }

    @Transactional
    void runQueries() {
        MyEntity_.managedBlocking().find("foo = 2");
        Assertions.assertEquals(1, MyEntity_.managedBlockingQueries().findFoos("fu").size());
        Assertions.assertEquals(1, MyEntity_.managedBlockingQueries().findFoosHQL("fu").size());
        Assertions.assertEquals(1, MyEntity_.managedBlockingQueries().findFoosFind("fu").size());
    }

    @Test
    void testRepositories() {
        clear();
        createOne();
        modifyOne();
        modifyOneCheck();
        modifyOneStatelessNoUpdate();
        modifyOneStateless();
        modifyOneStatelessCheck();
        clear();
        upsertNew();
        upsertExisting();
        upsertCheck();
        runQueries();
    }

}
