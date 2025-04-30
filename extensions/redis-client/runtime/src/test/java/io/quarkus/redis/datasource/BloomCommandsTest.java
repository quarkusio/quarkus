package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.BloomCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

@RequiresCommand("bf.add")
public class BloomCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    private BloomCommands<String, Person> bloom;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        bloom = ds.bloom(Person.class);
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(bloom.getDataSource());
    }

    @Test
    void bfadd() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        assertThat(bloom.bfadd(key, luke)).isTrue();

        assertThat(bloom.bfexists(key, luke)).isTrue();
        assertThat(bloom.bfexists(key, leia)).isFalse();

        assertThat(bloom.bfadd(key, luke)).isFalse();
    }

    @Test
    void bfmadd() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThat(bloom.bfmadd(key, luke, leia)).containsExactly(true, true);
        assertThat(bloom.bfexists(key, luke)).isTrue();
        assertThat(bloom.bfexists(key, leia)).isTrue();

        assertThat(bloom.bfmexists(key, luke, anakin, leia)).containsExactly(true, false, true);
        assertThat(bloom.bfmadd(key, luke, anakin)).containsExactly(false, true);
    }

    @Test
    void bfreserve() {
        bloom.bfreserve(key, 0.0001, 600000);
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");
        assertThat(bloom.bfmadd(key, luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(bloom.bfmexists(key, luke, anakin, leia)).containsExactly(true, true, true);

        assertThatThrownBy(() -> bloom.bfreserve(key, 0.0001, 6000, new BfReserveArgs().nonScaling()));

        bloom.bfreserve("key2", 0.0001, 6000, new BfReserveArgs().nonScaling());
        assertThat(bloom.bfmadd("key2", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(bloom.bfmexists("key2", luke, anakin, leia)).containsExactly(true, true, true);

        bloom.bfreserve("key3", 0.0001, 6000, new BfReserveArgs().expansion(5));
        assertThat(bloom.bfmadd("key3", luke, leia, anakin)).containsExactly(true, true, true);
        assertThat(bloom.bfmexists("key3", luke, anakin, leia)).containsExactly(true, true, true);
    }

    @Test
    void bfinsert() {
        Person luke = new Person("luke", "skywalker");
        Person leia = new Person("leia", "ordana");
        Person anakin = new Person("anakin", "skywalker");

        assertThat(bloom.bfinsert(key, new BfInsertArgs().capacity(600000).errorRate(0.0001).nonScaling(), luke, leia))
                .containsExactly(true, true);

        assertThat(bloom.bfexists(key, luke)).isTrue();
        assertThat(bloom.bfexists(key, leia)).isTrue();
        assertThat(bloom.bfexists(key, anakin)).isFalse();
        assertThat(bloom.bfadd(key, luke)).isFalse();

        assertThatThrownBy(() -> bloom.bfinsert("key2",
                new BfInsertArgs().capacity(600000).errorRate(0.0001).nonScaling().nocreate(), luke, leia, anakin));
        assertThatThrownBy(() -> bloom.bfinsert("key3", new BfInsertArgs().capacity(600000).errorRate(0.0001).expansion(1)));
    }

    @Test
    void bloomWithTypeReference() {
        var bloom = ds.bloom(new TypeReference<List<Person>>() {
            // Empty on purpose.
        });
        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person1, Person.person3);
        assertThat(bloom.bfadd(key, l1)).isTrue();

        assertThat(bloom.bfexists(key, l1)).isTrue();
        assertThat(bloom.bfexists(key, l2)).isFalse();

        assertThat(bloom.bfadd(key, l1)).isFalse();
        assertThat(bloom.bfadd(key, l2)).isTrue();
    }

}
