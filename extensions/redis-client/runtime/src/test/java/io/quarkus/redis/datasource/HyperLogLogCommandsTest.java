package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

public class HyperLogLogCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static AtomicInteger count = new AtomicInteger(0);
    private HyperLogLogCommands<String, Person> hll;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        hll = ds.hyperloglog(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(hll.getDataSource());
    }

    @Test
    void pfadd() {
        String k = getKey();
        assertThat(hll.pfadd(k, Person.person1, Person.person1)).isTrue();
        assertThat(hll.pfadd(k, Person.person1, Person.person1)).isFalse();
        Assertions.assertThat(hll.pfadd(k, Person.person1)).isFalse();
    }

    @Test
    void pfaddNoValues() {
        assertThatThrownBy(() -> hll.pfadd(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    @Test
    void pfaddNullValues() {
        assertThatThrownBy(() -> hll.pfadd(key, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("`values`");

        assertThatThrownBy(() -> hll.pfadd(key, Person.person1, null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("`values`");
    }

    private String getKey() {
        return "key-hll-" + count.getAndIncrement();
    }

    @Test
    void pfmerge() {
        String k1 = getKey();
        String k2 = getKey();
        String k3 = getKey();
        hll.pfadd(k1, Person.person1);
        hll.pfadd(k2, new Person("Bossk", ""));
        hll.pfadd(k3, new Person("Lando", "Calrissian"));

        hll.pfmerge(k1, k2, k3);
        assertThat(hll.pfcount(k1)).isEqualTo(3);

        String k4 = getKey();
        String k5 = getKey();
        hll.pfadd(k4, new Person("Lobot", ""), new Person("Ackbar", ""));
        hll.pfadd(k5, new Person("Ackbar", ""), new Person("Mon", "Mothma"));

        String k6 = getKey();
        hll.pfmerge(k6, k4, k5);

        assertThat(hll.pfcount(k6)).isEqualTo(3);
    }

    @Test
    void pfmergeNoKeys() {
        assertThatThrownBy(() -> hll.pfmerge(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfcount() {
        String k0 = getKey();
        String k1 = getKey();
        hll.pfadd(k0, Person.person1);
        hll.pfadd(k1, Person.person2);
        assertThat(hll.pfcount(k0)).isEqualTo(1);
        assertThat(hll.pfcount(k0, k1)).isEqualTo(2);
    }

    @Test
    void pfcountNoKeys() {
        assertThatThrownBy(() -> hll.pfcount()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfaddPfmergePfCount() {
        String k0 = getKey();
        String k1 = getKey();
        String k2 = getKey();
        hll.pfadd(k0, new Person("Lobot", ""), new Person("Ackbar", ""));
        hll.pfadd(k1, new Person("Ackbar", ""), new Person("Mon", "Mothma"));

        hll.pfmerge(k2, k0, k1);

        assertThat(hll.pfcount(k2)).isEqualTo(3);
    }

    @Test
    void pfaddWithTypeReference() {
        String k = getKey();
        var hll = ds.hyperloglog(new TypeReference<List<Person>>() {
            // Empty on purpose
        });
        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person3, Person.person2);
        assertThat(hll.pfadd(k, l1, l2)).isTrue();
        assertThat(hll.pfadd(k, l1, l1)).isFalse();
        Assertions.assertThat(hll.pfadd(k, l1)).isFalse();
    }

}
