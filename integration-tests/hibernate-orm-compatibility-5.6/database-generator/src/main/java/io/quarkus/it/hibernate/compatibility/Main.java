package io.quarkus.it.hibernate.compatibility;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        System.out.println("Initializing schema...");
        Quarkus.run(QuarkusMain.class, args);
    }

    static class QuarkusMain implements QuarkusApplication {
        @Inject
        EntityManager em;

        @Override
        public int run(String... args) {
            System.out.println("Initializing data...");
            MyEntity createdEntity = QuarkusTransaction.requiringNew().call(() -> {
                var entity = new MyEntity();
                entity.duration = Duration.of(59, ChronoUnit.SECONDS);
                entity.uuid = UUID.fromString("f49c6ba8-8d7f-417a-a255-d594dddf729f");
                entity.instant = Instant.parse("2018-01-01T10:58:30.00Z");
                entity.offsetTime = LocalTime.of(12, 58, 30, 0)
                        .atOffset(ZoneOffset.ofHours(2));
                entity.offsetDateTime = LocalDateTime.of(2018, 1, 1, 12, 58, 30, 0)
                        .atOffset(ZoneOffset.ofHours(2));
                entity.zonedDateTime = LocalDateTime.of(2018, 1, 1, 12, 58, 30, 0)
                        .atZone(ZoneId.of("Africa/Cairo" /* UTC+2 */));
                entity.intArray = new int[] { 0, 1, 42 };
                entity.stringList = new ArrayList<>(List.of("one", "two"));
                entity.myEnum = MyEnum.VALUE2;
                em.persist(entity);

                // Create more than one entity of each type,
                // so that we avoid the (uninteresting) edge case in sequence optimizers
                // where only 1 entity was created and the optimizer is just about to start another pool.
                em.persist(new MyEntity());
                em.persist(new MyEntityWithGenericGeneratorAndDefaultAllocationSize());
                em.persist(new MyEntityWithGenericGeneratorAndDefaultAllocationSize());
                em.persist(new MyEntityWithSequenceGeneratorAndDefaultAllocationSize());
                em.persist(new MyEntityWithSequenceGeneratorAndDefaultAllocationSize());

                return entity;
            });

            System.out.println("Checking data...");
            // Check that Hibernate ORM 5 used to load the values we're going to expect in compatibility tests
            QuarkusTransaction.requiringNew().run(() -> {
                checkEqual(1L, createdEntity.id);
                var loadedEntity = em.find(MyEntity.class, createdEntity.id);
                checkEqual(createdEntity.duration, loadedEntity.duration);
                checkEqual(createdEntity.uuid, loadedEntity.uuid);
                checkEqual(createdEntity.instant, loadedEntity.instant);
                checkEqual(createdEntity.offsetTime.toLocalTime().atOffset(ZoneId.systemDefault().getRules().getOffset(Instant.now())),
                        loadedEntity.offsetTime);
                checkEqual(createdEntity.offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime(),
                        loadedEntity.offsetDateTime);
                checkEqual(createdEntity.zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()), loadedEntity.zonedDateTime);
                checkEqual(createdEntity.intArray, loadedEntity.intArray);
                checkEqual(createdEntity.stringList, loadedEntity.stringList);
                checkEqual(createdEntity.myEnum, loadedEntity.myEnum);
            });

            System.out.println("Done.");
            return 0;
        }

        private <T> void checkEqual(T expected, T actual) {
            if (!Objects.equals(expected, actual)) {
                throw new AssertionError("Not equal; expected: " + expected + ", actual: " + actual);
            }
        }

        private void checkEqual(int[] expected, int[] actual) {
            if (!Arrays.equals(expected, actual)) {
                throw new AssertionError("Not equal; expected: " + Arrays.toString(expected)
                        + ", actual: " + Arrays.toString(actual));
            }
        }
    }
}
