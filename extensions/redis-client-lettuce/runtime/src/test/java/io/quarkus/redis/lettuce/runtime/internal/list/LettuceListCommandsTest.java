package io.quarkus.redis.lettuce.runtime.internal.list;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import io.quarkus.redis.lettuce.runtime.internal.Person;
import tools.jackson.core.type.TypeReference;

class LettuceListCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveListCommands<String, Person> reactiveLists;
    ListCommands<String, Person> blockingLists;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveLists = reactiveDs.list(Person.class);
        blockingLists = blockingDs.list(Person.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveLists.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingLists.getDataSource());
    }

    @Test
    void blpop() {
        blockingLists.rpush("two", Person.person2, Person.person3);
        assertThat(blockingLists.blpop(Duration.ofSeconds(1), "one", "two")).isEqualTo(KeyValue.of("two", Person.person2));
    }

    @Test
    void blmpop() {
        blockingLists.rpush("two", Person.person1, Person.person2, Person.person3);
        assertThat(blockingLists.blmpop(Duration.ofSeconds(1), Position.RIGHT, "one", "two"))
                .isEqualTo(KeyValue.of("two", Person.person3));
        assertThat(blockingLists.blmpop(Duration.ofSeconds(1), Position.LEFT, "one", "two"))
                .isEqualTo(KeyValue.of("two", Person.person1));
        assertThat(blockingLists.blmpop(Duration.ofSeconds(1), Position.LEFT, "one", "two"))
                .isEqualTo(KeyValue.of("two", Person.person2));
        assertThat(blockingLists.blmpop(Duration.ofSeconds(1), Position.LEFT, "one", "two")).isNull();
    }

    @Test
    void blmpopMany() {
        blockingLists.rpush("two", Person.person1, Person.person2, Person.person3);
        assertThat(blockingLists.blmpop(Duration.ofSeconds(1), Position.RIGHT, 2, "one", "two"))
                .containsExactly(KeyValue.of("two", Person.person3), KeyValue.of("two", Person.person2));
        assertThat(blockingLists.blmpop(Duration.ofSeconds(1), Position.RIGHT, 2, "one", "two"))
                .containsExactly(KeyValue.of("two", Person.person1));
        assertThat(blockingLists.blmpop(Duration.ofSeconds(1), Position.RIGHT, 2, "one", "two")).isEmpty();
    }

    @Test
    void blpopTimeout() {
        assertThat(blockingLists.blpop(Duration.ofSeconds(1), key)).isNull();
    }

    @Test
    void blpopWithFractionalTimeout() {
        blockingLists.rpush("two", Person.person2);
        assertThat(blockingLists.blpop(Duration.ofMillis(1500), "one", "two")).isEqualTo(KeyValue.of("two", Person.person2));
        assertThat(blockingLists.blpop(Duration.ofMillis(100), "one", "two")).isNull();
    }

    @Test
    void brpop() {
        blockingLists.rpush("two", Person.person2, Person.person3);
        assertThat(blockingLists.brpop(Duration.ofSeconds(1), "one", "two")).isEqualTo(KeyValue.of("two", Person.person3));
    }

    @Test
    void brpopTimeout() {
        assertThat(blockingLists.brpop(Duration.ofSeconds(1), key)).isNull();
    }

    @Test
    void brpoplpush() {
        blockingLists.rpush("one", Person.person1, Person.person2);
        blockingLists.rpush("two", Person.person3, Person.person4);
        assertThat(blockingLists.brpoplpush(Duration.ofSeconds(1), "one", "two")).isEqualTo(Person.person2);
        assertThat(blockingLists.lrange("one", 0, -1)).isEqualTo(List.of(Person.person1));
        assertThat(blockingLists.lrange("two", 0, -1)).isEqualTo(List.of(Person.person2, Person.person3, Person.person4));
    }

    @Test
    void blmove() {
        String list1 = key;
        String list2 = key + "-2";

        blockingLists.rpush(list1, Person.person1, Person.person2, Person.person3);
        blockingLists.blmove(list1, list2, Position.LEFT, Position.RIGHT, Duration.ofSeconds(1));

        assertThat(blockingLists.lrange(list1, 0, -1)).containsExactly(Person.person2, Person.person3);
        assertThat(blockingLists.lrange(list2, 0, -1)).containsOnly(Person.person1);
    }

    @Test
    void blmoveTimeout() {
        assertThat(blockingLists.blmove(key, key + "-2", Position.LEFT, Position.RIGHT, Duration.ofSeconds(1))).isNull();
    }

    @Test
    void lindex() {
        assertThat(blockingLists.lindex(key, 0)).isNull();
        blockingLists.rpush(key, Person.person1);
        assertThat(blockingLists.lindex(key, 0)).isEqualTo(Person.person1);
    }

    @Test
    void linsertBefore() {
        assertThat(blockingLists.linsertBeforePivot(key, Person.person1, Person.person2)).isEqualTo(0);
        blockingLists.rpush(key, Person.person1);
        blockingLists.rpush(key, Person.person3);
        assertThat(blockingLists.linsertBeforePivot(key, Person.person3, Person.person2)).isEqualTo(3);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2, Person.person3));
    }

    @Test
    void linsertAfter() {
        assertThat(blockingLists.linsertAfterPivot(key, Person.person1, Person.person2)).isEqualTo(0);
        blockingLists.rpush(key, Person.person1);
        blockingLists.rpush(key, Person.person3);
        assertThat(blockingLists.linsertAfterPivot(key, Person.person3, Person.person2)).isEqualTo(3);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person3, Person.person2));
    }

    @Test
    void llen() {
        assertThat(blockingLists.llen(key)).isEqualTo(0);
        blockingLists.lpush(key, Person.person1);
        assertThat(blockingLists.llen(key)).isEqualTo(1);
    }

    @Test
    void lmove() {
        String list1 = key;
        String list2 = key + "-2";

        blockingLists.rpush(list1, Person.person1, Person.person2, Person.person3);
        assertThat(blockingLists.lmove(list1, list2, Position.RIGHT, Position.LEFT)).isEqualTo(Person.person3);

        assertThat(blockingLists.lrange(list1, 0, -1)).containsExactly(Person.person1, Person.person2);
        assertThat(blockingLists.lrange(list2, 0, -1)).containsOnly(Person.person3);
    }

    @Test
    void lmoveOnMissingKey() {
        assertThat(blockingLists.lmove(key, key + "-2", Position.RIGHT, Position.LEFT)).isNull();
    }

    @Test
    void lpop() {
        assertThat(blockingLists.lpop(key)).isNull();
        blockingLists.rpush(key, Person.person1, Person.person2);
        assertThat(blockingLists.lpop(key)).isEqualTo(Person.person1);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person2));
    }

    @Test
    void lmpop() {
        assertThat(blockingLists.lmpop(Position.RIGHT, key)).isNull();
        blockingLists.rpush(key, Person.person1, Person.person2);
        assertThat(blockingLists.lmpop(Position.RIGHT, key)).isEqualTo(KeyValue.of(key, Person.person2));
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1));
    }

    @Test
    void lmpopMany() {
        assertThat(blockingLists.lmpop(Position.RIGHT, 2, key)).isEmpty();
        blockingLists.rpush(key, Person.person1, Person.person2);
        assertThat(blockingLists.lmpop(Position.RIGHT, 2, key)).containsExactly(KeyValue.of(key, Person.person2),
                KeyValue.of(key, Person.person1));
        assertThat(blockingLists.lrange(key, 0, -1)).isEmpty();
        assertThat(blockingLists.lmpop(Position.RIGHT, 2, key)).isEmpty();
    }

    @Test
    void lpopCount() {
        assertThat(blockingLists.lpop(key, 1)).isEqualTo(List.of());
        blockingLists.rpush(key, Person.person1, Person.person2);
        assertThat(blockingLists.lpop(key, 3)).isEqualTo(List.of(Person.person1, Person.person2));
    }

    @Test
    void lpos() {
        blockingLists.rpush(key, Person.person4, Person.person5, Person.person6, Person.person1, Person.person2,
                Person.person3, Person.person6, Person.person6);

        assertThat(blockingLists.lpos("nope", Person.person4)).isEmpty();
        assertThat(blockingLists.lpos(key, new Person("john", "doe"))).isEmpty();
        assertThat(blockingLists.lpos(key, Person.person4)).hasValue(0);
        assertThat(blockingLists.lpos(key, Person.person6)).hasValue(2);
        assertThat(blockingLists.lpos(key, Person.person6, new LPosArgs().rank(1))).hasValue(2);
        assertThat(blockingLists.lpos(key, Person.person6, new LPosArgs().rank(2))).hasValue(6);
        assertThat(blockingLists.lpos(key, Person.person6, new LPosArgs().rank(4))).isEmpty();

        assertThat(blockingLists.lpos(key, Person.person6, 0)).contains(2L, 6L, 7L);
        assertThat(blockingLists.lpos(key, Person.person6, 2)).containsExactly(2L, 6L);
        assertThat(blockingLists.lpos(key, Person.person6, 0, new LPosArgs().maxlen(1))).isEmpty();
        assertThat(blockingLists.lpos(key, Person.person6, 0, new LPosArgs().rank(-1))).containsExactly(7L, 6L, 2L);
    }

    @Test
    void lposReactiveReturnsNullWhenAbsent() {
        blockingLists.rpush(key, Person.person1);
        assertThat(reactiveLists.lpos(key, Person.person2).await().atMost(TIMEOUT)).isNull();
        assertThat(reactiveLists.lpos(key, Person.person2, new LPosArgs().rank(1)).await().atMost(TIMEOUT)).isNull();
        assertThat(reactiveLists.lpos(key, Person.person1).await().atMost(TIMEOUT)).isEqualTo(0L);
        assertThat(reactiveLists.lpos(key, Person.person2, 0).await().atMost(TIMEOUT)).isEmpty();
    }

    @Test
    void lpush() {
        assertThat(blockingLists.lpush(key, Person.person2)).isEqualTo(1);
        assertThat(blockingLists.lpush(key, Person.person1)).isEqualTo(2);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
        assertThat(blockingLists.lpush(key, Person.person3, Person.person4)).isEqualTo(4);
        assertThat(blockingLists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person4, Person.person3, Person.person1, Person.person2));
    }

    @Test
    void lpushx() {
        assertThat(blockingLists.lpushx(key, Person.person2)).isEqualTo(0);
        blockingLists.lpush(key, Person.person2);
        assertThat(blockingLists.lpushx(key, Person.person1)).isEqualTo(2);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
    }

    @Test
    void lpushxMultiple() {
        assertThat(blockingLists.lpushx(key, Person.person1, Person.person2)).isEqualTo(0);
        blockingLists.lpush(key, Person.person2);
        assertThat(blockingLists.lpushx(key, Person.person1, Person.person3)).isEqualTo(3);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person3, Person.person1, Person.person2));
    }

    @Test
    void lrange() {
        assertThat(blockingLists.lrange(key, 0, 10).isEmpty()).isTrue();
        blockingLists.rpush(key, Person.person1, Person.person2, Person.person3);
        List<Person> range = blockingLists.lrange(key, 0, 1);
        assertThat(range).hasSize(2);
        assertThat(range.get(0)).isEqualTo(Person.person1);
        assertThat(range.get(1)).isEqualTo(Person.person2);
        assertThat(blockingLists.lrange(key, 0, -1)).hasSize(3);
    }

    @Test
    void lrem() {
        assertThat(blockingLists.lrem(key, 0, Person.person6)).isEqualTo(0);

        blockingLists.rpush(key, Person.person1, Person.person2, Person.person1, Person.person2, Person.person1);
        assertThat(blockingLists.lrem(key, 1, Person.person1)).isEqualTo(1);
        assertThat(blockingLists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person2, Person.person1, Person.person2, Person.person1));

        blockingLists.lpush(key, Person.person1);
        assertThat(blockingLists.lrem(key, -1, Person.person1)).isEqualTo(1);
        assertThat(blockingLists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person1, Person.person2, Person.person1, Person.person2));

        blockingLists.lpush(key, Person.person1);
        assertThat(blockingLists.lrem(key, 0, Person.person1)).isEqualTo(3);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person2, Person.person2));
    }

    @Test
    void lset() {
        blockingLists.rpush(key, Person.person1, Person.person2, Person.person3);
        blockingLists.lset(key, 2, Person.person6);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2, Person.person6));
    }

    @Test
    void ltrim() {
        blockingLists.rpush(key, Person.person1, Person.person2, Person.person3, Person.person4, Person.person5,
                Person.person6);
        blockingLists.ltrim(key, 0, 3);
        assertThat(blockingLists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person1, Person.person2, Person.person3, Person.person4));
        blockingLists.ltrim(key, -2, -1);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person3, Person.person4));
    }

    @Test
    void rpop() {
        assertThat(blockingLists.rpop(key)).isNull();
        blockingLists.rpush(key, Person.person1, Person.person2);
        assertThat(blockingLists.rpop(key)).isEqualTo(Person.person2);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1));
    }

    @Test
    void rpopCount() {
        assertThat(blockingLists.rpop(key, 1)).isEqualTo(List.of());
        blockingLists.rpush(key, Person.person1, Person.person2);
        assertThat(blockingLists.rpop(key, 3)).isEqualTo(List.of(Person.person2, Person.person1));
    }

    @Test
    void rpoplpush() {
        assertThat(blockingLists.rpoplpush("one", "two")).isNull();
        blockingLists.rpush("one", Person.person1, Person.person2);
        blockingLists.rpush("two", Person.person3, Person.person4);
        assertThat(blockingLists.rpoplpush("one", "two")).isEqualTo(Person.person2);
        assertThat(blockingLists.lrange("one", 0, -1)).isEqualTo(List.of(Person.person1));
        assertThat(blockingLists.lrange("two", 0, -1)).isEqualTo(List.of(Person.person2, Person.person3, Person.person4));
    }

    @Test
    void rpush() {
        assertThat(blockingLists.rpush(key, Person.person1)).isEqualTo(1);
        assertThat(blockingLists.rpush(key, Person.person2)).isEqualTo(2);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
        assertThat(blockingLists.rpush(key, Person.person3, Person.person4)).isEqualTo(4);
        assertThat(blockingLists.lrange(key, 0, -1))
                .isEqualTo(List.of(Person.person1, Person.person2, Person.person3, Person.person4));
    }

    @Test
    void rpushx() {
        assertThat(blockingLists.rpushx(key, Person.person1)).isEqualTo(0);
        blockingLists.rpush(key, Person.person1);
        assertThat(blockingLists.rpushx(key, Person.person2)).isEqualTo(2);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2));
    }

    @Test
    void rpushxMultiple() {
        assertThat(blockingLists.rpushx(key, Person.person2, Person.person3)).isEqualTo(0);
        blockingLists.rpush(key, Person.person1);
        assertThat(blockingLists.rpushx(key, Person.person2, Person.person3)).isEqualTo(3);
        assertThat(blockingLists.lrange(key, 0, -1)).isEqualTo(List.of(Person.person1, Person.person2, Person.person3));
    }

    @Test
    void sort() {
        ListCommands<String, String> commands = blockingDs.list(String.class, String.class);
        commands.rpush(key, "9", "5", "1", "3", "5", "8", "7", "6", "2", "4");

        assertThat(commands.sort(key)).containsExactly("1", "2", "3", "4", "5", "5", "6", "7", "8", "9");

        assertThat(commands.sort(key, new SortArgs().descending())).containsExactly("9", "8", "7", "6", "5", "5", "4",
                "3", "2", "1");
        assertThat(commands.sort(key, new SortArgs().limit(0, 3))).containsExactly("1", "2", "3");

        String k = key + "-alpha";
        commands.rpush(k, "a", "e", "f", "b");

        assertThat(commands.sort(k, new SortArgs().alpha())).containsExactly("a", "b", "e", "f");

        commands.sortAndStore(k, "dest1", new SortArgs().alpha());
        commands.sortAndStore(key, "dest2");

        assertThat(commands.lpop("dest1", 100)).containsExactly("a", "b", "e", "f");
        assertThat(commands.lpop("dest2", 100)).containsExactly("1", "2", "3", "4", "5", "5", "6", "7", "8", "9");
    }

    @Test
    void sortOnMissingKey() {
        assertThat(blockingLists.sort(key)).isEmpty();
    }

    @Test
    void sortAndStore() {
        ListCommands<String, String> commands = blockingDs.list(String.class, String.class);
        String alphaKey = key + "-alpha";
        commands.rpush(key, "9", "5", "1", "3", "5", "8", "7", "6", "2", "4");
        commands.rpush(alphaKey, "a", "e", "f", "b");

        assertThat(commands.sortAndStore(alphaKey, "dest1", new SortArgs().alpha())).isEqualTo(4);
        assertThat(commands.sortAndStore(key, "dest2")).isEqualTo(10);
    }

    @Test
    void testListWithTypeReference() {
        var lists = blockingDs.list(new TypeReference<List<Person>>() {
            // Empty on purpose
        });

        var l1 = List.of(Person.person1, Person.person2);
        var l2 = List.of(Person.person1, Person.person3);

        lists.rpush(key, l1, l2);
        assertThat(lists.blpop(Duration.ofSeconds(1), "one", key)).isEqualTo(KeyValue.of(key, l1));
    }

    /** Covers the {@code TypeReference} overloads the Vert.x-mirrored test above does not reach. */
    @Test
    void listWithKeyAndValueTypeReferences() {
        ListCommands<String, String> commands = blockingDs.list(new TypeReference<>() {
            // Empty on purpose
        });
        commands.rpush(key, "v1", "v2");
        assertThat(commands.blpop(Duration.ofSeconds(1), "one", key)).isEqualTo(KeyValue.of(key, "v1"));

        ListCommands<String, String> keyAndValue = blockingDs.list(new TypeReference<>() {
            // Empty on purpose
        }, new TypeReference<>() {
            // Empty on purpose
        });
        assertThat(keyAndValue.lrange(key, 0, -1)).containsExactly("v2");

        ReactiveListCommands<String, String> reactiveCommands = reactiveDs.list(new TypeReference<>() {
            // Empty on purpose
        });
        assertThat(reactiveCommands.llen(key).await().atMost(TIMEOUT)).isEqualTo(1L);
    }

    @Test
    void testJacksonPolymorphism() {
        var cmd = blockingDs.list(Animal.class);

        var cat = new Cat();
        cat.setId("1234");
        cat.setName("the cat");

        var rabbit = new Rabbit();
        rabbit.setName("roxanne");
        rabbit.setColor("grey");

        cmd.lpush(key, cat, rabbit);

        var shouldBeACat = cmd.rpop(key);
        var shouldBeARabbit = cmd.rpop(key);

        assertThat(shouldBeACat).isInstanceOf(Cat.class)
                .satisfies(animal -> {
                    assertThat(animal.getName()).isEqualTo("the cat");
                    assertThat(((Cat) animal).getId()).isEqualTo("1234");
                });

        assertThat(shouldBeARabbit).isInstanceOf(Rabbit.class)
                .satisfies(animal -> {
                    assertThat(animal.getName()).isEqualTo("roxanne");
                    assertThat(((Rabbit) animal).getColor()).isEqualTo("grey");
                });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Cat.class, name = "Cat"),
            @JsonSubTypes.Type(value = Rabbit.class, name = "Rabbit")
    })
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
