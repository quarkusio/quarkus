package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.CuckooCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@RequiresCommand("cf.add")
public class CuckooCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private CuckooCommands<String, Person> cuckoo;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        cuckoo = ds.cuckoo(Person.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(cuckoo.getDataSource());
    }

    @Test
    void cfadd() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        cuckoo.cfadd(key, luke);
        cuckoo.cfadd(key, luke);
        cuckoo.cfadd(key, leia);

        assertThat(cuckoo.cfexists(key, luke)).isTrue();
        assertThat(cuckoo.cfexists(key, leia)).isTrue();
        assertThat(cuckoo.cfexists(key, anakin)).isFalse();

        assertThat(cuckoo.cfaddnx(key, leia)).isFalse();
        assertThat(cuckoo.cfaddnx(key, anakin)).isTrue();
        assertThat(cuckoo.cfexists(key, anakin)).isTrue();

        assertThat(cuckoo.cfcount(key, leia)).isEqualTo(1);
        assertThat(cuckoo.cfcount(key, luke)).isEqualTo(2);
        assertThat(cuckoo.cfcount(key, anakin)).isEqualTo(1);
        assertThat(cuckoo.cfcount(key, new Person("john", "doe"))).isEqualTo(0);
    }

    @Test
    void cfdel() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        cuckoo.cfadd(key, luke);
        cuckoo.cfadd(key, luke);
        cuckoo.cfadd(key, leia);

        assertThat(cuckoo.cfexists(key, luke)).isTrue();
        assertThat(cuckoo.cfexists(key, leia)).isTrue();
        assertThat(cuckoo.cfexists(key, anakin)).isFalse();
        assertThat(cuckoo.cfmexists(key, leia, luke, anakin)).containsExactly(true, true, false);

        assertThat(cuckoo.cfdel(key, luke)).isTrue();
        assertThat(cuckoo.cfdel(key, luke)).isTrue();
        assertThat(cuckoo.cfdel(key, luke)).isFalse();

        assertThat(cuckoo.cfdel(key, anakin)).isFalse();

        assertThatThrownBy(() -> cuckoo.cfdel("missing", anakin)).hasMessageContaining("Not found");
    }

    @Test
    void cfinsert() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        cuckoo.cfinsert(key, new CfInsertArgs().capacity(600000), luke, leia, leia);

        assertThat(cuckoo.cfexists(key, luke)).isTrue();
        assertThat(cuckoo.cfexists(key, leia)).isTrue();
        assertThat(cuckoo.cfexists(key, anakin)).isFalse();
        assertThat(cuckoo.cfcount(key, leia)).isEqualTo(2);
        assertThat(cuckoo.cfcount(key, luke)).isEqualTo(1);
        assertThat(cuckoo.cfaddnx(key, luke)).isFalse();

        assertThatThrownBy(
                () -> cuckoo.cfinsert("key2", new CfInsertArgs().capacity(600000).nocreate(), luke, leia, anakin));
        assertThatThrownBy(() -> cuckoo.cfinsert("key3", new CfInsertArgs().capacity(600000)));
    }

    @Test
    void cfinsertnx() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThat(cuckoo.cfinsertnx(key, new CfInsertArgs().capacity(600000), luke, leia, leia)).containsExactly(true,
                true, false);

        assertThat(cuckoo.cfexists(key, luke)).isTrue();
        assertThat(cuckoo.cfexists(key, leia)).isTrue();
        assertThat(cuckoo.cfexists(key, anakin)).isFalse();
        assertThat(cuckoo.cfcount(key, leia)).isEqualTo(1);
        assertThat(cuckoo.cfcount(key, luke)).isEqualTo(1);
        assertThat(cuckoo.cfaddnx(key, luke)).isFalse();

        assertThatThrownBy(
                () -> cuckoo.cfinsertnx("key2", new CfInsertArgs().capacity(600000).nocreate(), luke, leia, anakin));
        assertThatThrownBy(() -> cuckoo.cfinsertnx("key3", new CfInsertArgs().capacity(600000)));
    }

    @Test
    void cfreserve() {
        cuckoo.cfreserve(key, 600000);
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        assertThat(cuckoo.cfinsertnx(key, luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(cuckoo.cfmexists(key, luke, anakin, leia)).containsExactly(true, true, true);

        assertThatThrownBy(() -> cuckoo.cfreserve(key, 6000, new CfReserveArgs().bucketSize(10)));

        cuckoo.cfreserve("key2", 6000, new CfReserveArgs().maxIterations(1));
        assertThat(cuckoo.cfinsertnx("key2", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(cuckoo.cfmexists("key2", luke, anakin, leia)).containsExactly(true, true, true);

        cuckoo.cfreserve("key3", 6000, new CfReserveArgs().expansion(5).bucketSize(1));
        assertThat(cuckoo.cfinsertnx("key3", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(cuckoo.cfmexists("key3", luke, anakin, leia)).containsExactly(true, true, true);
    }

    @Test
    void cuckooWithTypeReference() {
        var cuckoo = ds.cuckoo(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person1, Person.person3);
        cuckoo.cfadd(key, l1);

        assertThat(cuckoo.cfexists(key, l1)).isTrue();
        assertThat(cuckoo.cfexists(key, l2)).isFalse();

        assertThat(cuckoo.cfaddnx(key, l1)).isFalse();
        cuckoo.cfadd(key, l2);
        assertThat(cuckoo.cfexists(key, l2)).isTrue();
    }

}
