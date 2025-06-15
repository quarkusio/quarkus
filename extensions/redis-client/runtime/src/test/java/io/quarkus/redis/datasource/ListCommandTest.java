package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

public class ListCommandTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static String key = "key-list";
    private ListCommands<String, Person> lists;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(5));

        lists = ds.list(Person.class);
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(lists.getDataSource());
    }

    @Test
    void blpop() {
        lists.rpush("two", Person.person2, Person.person3);
        assertThat(lists.blpop(Duration.ofSeconds(1), "one", "two")).isEqualTo(KeyValue.of("two", Person.person2));
    }

    @Test
    @RequiresRedis7OrHigher
    void blmpop() {
        lists.rpush("two", Person.person1, Person.person2, Person.person3);
        assertThat(lists.blmpop(Duration.ofSeconds(1), Position.RIGHT, "one", "two"))
                .isEqualTo(KeyValue.of("two", Person.person3));
        assertThat(lists.blmpop(Duration.ofSeconds(1), Position.LEFT, "one", "two"))
                .isEqualTo(KeyValue.of("two", Person.person1));
        assertThat(lists.blmpop(Duration.ofSeconds(1), Position.LEFT, "one", "two"))
                .isEqualTo(KeyValue.of("two", Person.person2));
        assertThat(lists.blmpop(Duration.ofSeconds(1), Position.LEFT, "one", "two")).isNull();
    }

    @Test
    @RequiresRedis7OrHigher
    void blmpopMany() {
        lists.rpush("two", Person.person1, Person.person2, Person.person3);
        assertThat(lists.blmpop(Duration.ofSeconds(1), Position.RIGHT, 2, "one", "two"))
                .containsExactly(KeyValue.of("two", Person.person3), KeyValue.of("two", Person.person2));
        assertThat(lists.blmpop(Duration.ofSeconds(1), Position.RIGHT, 2, "one", "two"))
                .containsExactly(KeyValue.of("two", Person.person1));
        assertThat(lists.blmpop(Duration.ofSeconds(1), Position.RIGHT, 2, "one", "two")).isEmpty();
    }

    @Test
    void blpopTimeout() {
        assertThat(lists.blpop(Duration.ofSeconds(1), key)).isNull();
    }

    @Test
    void brpop() {
        lists.rpush("two", Person.person2, Person.person3);
        assertThat(lists.brpop(Duration.ofSeconds(1), "one", "two")).isEqualTo(KeyValue.of("two", Person.person3));
    }

    @Test
    void brpopDoubleTimeout() {
        lists.rpush("two", Person.person2, Person.person3);
        assertThat(lists.brpop(Duration.ofSeconds(1), "one", "two")).isEqualTo(KeyValue.of("two", Person.person3));
    }

    @Test
    void brpoplpush() {
        lists.rpush("one", Person.person1, Person.person2);
        lists.rpush("two", Person.person3, Person.person4);
        assertThat(lists.brpoplpush(Duration.ofSeconds(1), "one", "two")).isEqualTo(Person.person2);
        assertThat(lists.lrange("one", 0, -1)).isEqualTo(List.of(Person.person1));
        assertThat(lists.lrange("two", 0, -1)).isEqualTo(List.of(Person.person2, Person.person3, Person.person4));
    }

    @Test
    void lindex() {
        assertThat(lists.lindex(key, 0)).isNull();
        lists.rpush(key, Person.person1);
        assertThat(lists.lindex(key, 0)).isEqualTo(Person.person1);
    }

    @Test
    void linsertBefore() {
        assertThat(lists.linsertBeforePivot(key, Person.person1, Person.person2)).isEqualTo(0);
        lists.rpush(key, Person.person1);
        lists.rpush(key, Person.person3);
        assertThat(lists.linsertBeforePivot(key, Person.person3, Person.person2)).isEqualTo(3);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2, Person.person3));
    }

    @Test
    void linsertAfter() {
        assertThat(lists.linsertAfterPivot(key, Person.person1, Person.person2)).isEqualTo(0);
        lists.rpush(key, Person.person1);
        lists.rpush(key, Person.person3);
        assertThat(lists.linsertAfterPivot(key, Person.person3, Person.person2)).isEqualTo(3);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person3, Person.person2));
    }

    @Test
    void llen() {
        assertThat(lists.llen(key)).isEqualTo(0);
        lists.lpush(key, Person.person1);
        assertThat(lists.llen(key)).isEqualTo(1);
    }

    @Test
    void lpop() {
        assertThat(lists.lpop(key)).isNull();
        lists.rpush(key, Person.person1, Person.person2);
        assertThat(lists.lpop(key)).isEqualTo(Person.person1);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person2));
    }

    @Test
    @RequiresRedis7OrHigher
    void lmpop() {
        assertThat(lists.lmpop(Position.RIGHT, key)).isNull();
        lists.rpush(key, Person.person1, Person.person2);
        assertThat(lists.lmpop(Position.RIGHT, key)).isEqualTo(KeyValue.of(key, Person.person2));
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1));
    }

    @Test
    @RequiresRedis7OrHigher
    void lmpopMany() {
        assertThat(lists.lmpop(Position.RIGHT, 2, key)).isEmpty();
        lists.rpush(key, Person.person1, Person.person2);
        assertThat(lists.lmpop(Position.RIGHT, 2, key)).containsExactly(KeyValue.of(key, Person.person2),
                KeyValue.of(key, Person.person1));
        assertThat(lists.lrange(key, 0, -1)).isEmpty();
        assertThat(lists.lmpop(Position.RIGHT, 2, key)).isEmpty();
    }

    @Test
    @RequiresRedis6OrHigher
    void lpopCount() {
        assertThat(lists.lpop(key, 1)).isEqualTo(List.of());
        lists.rpush(key, Person.person1, Person.person2);
        assertThat(lists.lpop(key, 3)).isEqualTo(List.of(Person.person1, Person.person2));
    }

    @Test
    @RequiresRedis6OrHigher
    void lpos() {

        lists.rpush(key, Person.person4, Person.person5, Person.person6, Person.person1, Person.person2, Person.person3,
                Person.person6, Person.person6);

        assertThat(lists.lpos("nope", Person.person4)).isEmpty();
        assertThat(lists.lpos(key, new Person("john", "doe"))).isEmpty();
        assertThat(lists.lpos(key, Person.person4)).hasValue(0);
        assertThat(lists.lpos(key, Person.person6)).hasValue(2);
        Assertions.assertThat(lists.lpos(key, Person.person6, new LPosArgs().rank(1))).hasValue(2);
        Assertions.assertThat(lists.lpos(key, Person.person6, new LPosArgs().rank(2))).hasValue(6);
        Assertions.assertThat(lists.lpos(key, Person.person6, new LPosArgs().rank(4))).isEmpty();

        Assertions.assertThat(lists.lpos(key, Person.person6, 0)).contains(2L, 6L, 7L);
        assertThat(lists.lpos(key, Person.person6, 0, new LPosArgs().maxlen(1))).isEmpty();
    }

    @Test
    void lpush() {
        Assertions.assertThat(lists.lpush(key, Person.person2)).isEqualTo(1);
        Assertions.assertThat(lists.lpush(key, Person.person1)).isEqualTo(2);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
        assertThat(lists.lpush(key, Person.person3, Person.person4)).isEqualTo(4);
        assertThat(lists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person4, Person.person3, Person.person1, Person.person2));
    }

    @Test
    void lpushx() {
        Assertions.assertThat(lists.lpushx(key, Person.person2)).isEqualTo(0);
        lists.lpush(key, Person.person2);
        Assertions.assertThat(lists.lpushx(key, Person.person1)).isEqualTo(2);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
    }

    @Test
    void lpushxMultiple() {
        assertThat(lists.lpushx(key, Person.person1, Person.person2)).isEqualTo(0);
        lists.lpush(key, Person.person2);
        assertThat(lists.lpushx(key, Person.person1, Person.person3)).isEqualTo(3);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person3, Person.person1, Person.person2));
    }

    @Test
    void lrange() {
        assertThat(lists.lrange(key, 0, 10).isEmpty()).isTrue();
        lists.rpush(key, Person.person1, Person.person2, Person.person3);
        List<Person> range = lists.lrange(key, 0, 1);
        assertThat(range).hasSize(2);
        assertThat(range.get(0)).isEqualTo(Person.person1);
        assertThat(range.get(1)).isEqualTo(Person.person2);
        assertThat(lists.lrange(key, 0, -1)).hasSize(3);
    }

    @Test
    void lrem() {
        assertThat(lists.lrem(key, 0, Person.person6)).isEqualTo(0);

        lists.rpush(key, Person.person1, Person.person2, Person.person1, Person.person2, Person.person1);
        assertThat(lists.lrem(key, 1, Person.person1)).isEqualTo(1);
        assertThat(lists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person2, Person.person1, Person.person2, Person.person1));

        lists.lpush(key, Person.person1);
        assertThat(lists.lrem(key, -1, Person.person1)).isEqualTo(1);
        assertThat(lists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person1, Person.person2, Person.person1, Person.person2));

        lists.lpush(key, Person.person1);
        assertThat(lists.lrem(key, 0, Person.person1)).isEqualTo(3);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person2, Person.person2));
    }

    @Test
    void lset() {
        lists.rpush(key, Person.person1, Person.person2, Person.person3);
        lists.lset(key, 2, Person.person6);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2, Person.person6));
    }

    @Test
    void ltrim() {
        lists.rpush(key, Person.person1, Person.person2, Person.person3, Person.person4, Person.person5,
                Person.person6);
        lists.ltrim(key, 0, 3);
        assertThat(lists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person1, Person.person2, Person.person3, Person.person4));
        lists.ltrim(key, -2, -1);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person3, Person.person4));
    }

    @Test
    void rpop() {
        assertThat(lists.rpop(key)).isNull();
        lists.rpush(key, Person.person1, Person.person2);
        assertThat(lists.rpop(key)).isEqualTo(Person.person2);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1));
    }

    @Test
    @RequiresRedis6OrHigher
    void rpopCount() {
        assertThat(lists.rpop(key, 1)).isEqualTo(List.of());
        lists.rpush(key, Person.person1, Person.person2);
        assertThat(lists.rpop(key, 3)).isEqualTo(List.of(Person.person2, Person.person1));
    }

    @Test
    void rpoplpush() {
        assertThat(lists.rpoplpush("one", "two")).isNull();
        lists.rpush("one", Person.person1, Person.person2);
        lists.rpush("two", Person.person3, Person.person4);
        assertThat(lists.rpoplpush("one", "two")).isEqualTo(Person.person2);
        assertThat(lists.lrange("one", 0, -1)).isEqualTo(List.of(Person.person1));
        assertThat(lists.lrange("two", 0, -1)).isEqualTo(List.of(Person.person2, Person.person3, Person.person4));
    }

    @Test
    void rpush() {
        Assertions.assertThat(lists.rpush(key, Person.person1)).isEqualTo(1);
        Assertions.assertThat(lists.rpush(key, Person.person2)).isEqualTo(2);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
        assertThat(lists.rpush(key, Person.person3, Person.person4)).isEqualTo(4);
        assertThat(lists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person1, Person.person2, Person.person3, Person.person4));
    }

    @Test
    void rpushx() {
        Assertions.assertThat(lists.rpushx(key, Person.person1)).isEqualTo(0);
        lists.rpush(key, Person.person1);
        Assertions.assertThat(lists.rpushx(key, Person.person2)).isEqualTo(2);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
    }

    @Test
    void rpushxMultiple() {
        assertThat(lists.rpushx(key, Person.person2, Person.person3)).isEqualTo(0);
        lists.rpush(key, Person.person1);
        assertThat(lists.rpushx(key, Person.person2, Person.person3)).isEqualTo(3);
        assertThat(lists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2, Person.person3));
    }

    @Test
    @RequiresRedis6OrHigher
    void lmove() {
        String list1 = key;
        String list2 = key + "-2";

        lists.rpush(list1, Person.person1, Person.person2, Person.person3);
        lists.lmove(list1, list2, Position.RIGHT, Position.LEFT);

        assertThat(lists.lrange(list1, 0, -1)).containsExactly(Person.person1, Person.person2);
        assertThat(lists.lrange(list2, 0, -1)).containsOnly(Person.person3);
    }

    @Test
    @RequiresRedis6OrHigher
    void blmove() {
        String list1 = key;
        String list2 = key + "-2";

        lists.rpush(list1, Person.person1, Person.person2, Person.person3);
        lists.blmove(list1, list2, Position.LEFT, Position.RIGHT, Duration.ofSeconds(1));

        assertThat(lists.lrange(list1, 0, -1)).containsExactly(Person.person2, Person.person3);
        assertThat(lists.lrange(list2, 0, -1)).containsOnly(Person.person1);
    }

    @Test
    @RequiresRedis6OrHigher
    void sort() {
        ListCommands<String, String> commands = ds.list(String.class, String.class);
        commands.rpush(key, "9", "5", "1", "3", "5", "8", "7", "6", "2", "4");

        assertThat(commands.sort(key)).containsExactly("1", "2", "3", "4", "5", "5", "6", "7", "8", "9");

        assertThat(commands.sort(key, new SortArgs().descending())).containsExactly("9", "8", "7", "6", "5", "5", "4",
                "3", "2", "1");

        String k = key + "-alpha";
        commands.rpush(k, "a", "e", "f", "b");

        assertThat(commands.sort(k, new SortArgs().alpha())).containsExactly("a", "b", "e", "f");

        commands.sortAndStore(k, "dest1", new SortArgs().alpha());
        commands.sortAndStore(key, "dest2");

        assertThat(commands.lpop("dest1", 100)).containsExactly("a", "b", "e", "f");
        assertThat(commands.lpop("dest2", 100)).containsExactly("1", "2", "3", "4", "5", "5", "6", "7", "8", "9");
    }

    @Test
    void testListWithTypeReference() {
        var lists = ds.list(new TypeReference<List<Person>>() {
            // Empty on purpose
        });

        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person1, Person.person3);

        lists.rpush(key, l1, l2);
        assertThat(lists.blpop(Duration.ofSeconds(1), "one", key)).isEqualTo(KeyValue.of(key, l1));
    }

    @Test
    void testJacksonPolymorphism() {
        var cmd = ds.list(Animal.class);

        var cat = new Cat();
        cat.setId("1234");
        cat.setName("the cat");

        var rabbit = new Rabbit();
        rabbit.setName("roxanne");
        rabbit.setColor("grey");

        cmd.lpush(key, cat, rabbit);

        var shouldBeACat = cmd.rpop(key);
        var shouldBeARabbit = cmd.rpop(key);

        assertThat(shouldBeACat).isInstanceOf(Cat.class).satisfies(animal -> {
            assertThat(animal.getName()).isEqualTo("the cat");
            assertThat(((Cat) animal).getId()).isEqualTo("1234");
        });

        assertThat(shouldBeARabbit).isInstanceOf(Rabbit.class).satisfies(animal -> {
            assertThat(animal.getName()).isEqualTo("roxanne");
            assertThat(((Rabbit) animal).getColor()).isEqualTo("grey");
        });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonSubTypes({ @JsonSubTypes.Type(value = Cat.class, name = "Cat"),
            @JsonSubTypes.Type(value = Rabbit.class, name = "Rabbit") })
    public static class Animal {

        private String name;

        public String getName() {
            return name;
        }

        public Animal setName(String name) {
            this.name = name;
            return this;
        }
    }

    public static class Rabbit extends Animal {

        private String color;

        public String getColor() {
            return color;
        }

        public Rabbit setColor(String color) {
            this.color = color;
            return this;
        }
    }

    public static class Cat extends Animal {
        private String id;

        public String getId() {
            return id;
        }

        public Cat setId(String id) {
            this.id = id;
            return this;
        }
    }
}
