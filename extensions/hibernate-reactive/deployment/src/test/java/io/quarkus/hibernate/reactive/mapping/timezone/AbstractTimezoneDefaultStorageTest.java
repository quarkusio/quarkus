package io.quarkus.hibernate.reactive.mapping.timezone;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.inject.Inject;

import org.assertj.core.api.SoftAssertions;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.SchemaUtil;
import io.quarkus.test.vertx.UniAsserter;

public class AbstractTimezoneDefaultStorageTest {

    private static final LocalDateTime LOCAL_DATE_TIME_TO_TEST = LocalDateTime.of(2017, Month.NOVEMBER, 6, 19, 19, 0);
    public static final ZonedDateTime PERSISTED_ZONED_DATE_TIME = LOCAL_DATE_TIME_TO_TEST.atZone(ZoneId.of("Africa/Cairo"));
    public static final OffsetDateTime PERSISTED_OFFSET_DATE_TIME = LOCAL_DATE_TIME_TO_TEST.atOffset(ZoneOffset.ofHours(3));
    public static final OffsetTime PERSISTED_OFFSET_TIME = LOCAL_DATE_TIME_TO_TEST.toLocalTime()
            .atOffset(ZoneOffset.ofHours(3));

    @Inject
    Mutiny.SessionFactory sessionFactory;

    MappingMetamodel mappingMetamodel() {
        return SchemaUtil.mappingMetamodel(sessionFactory);
    }

    protected void assertPersistedThenLoadedValues(UniAsserter asserter, ZonedDateTime expectedZonedDateTime,
            OffsetDateTime expectedOffsetDateTime, OffsetTime expectedOffsetTime) {
        asserter.assertThat(
                () -> sessionFactory.withTransaction(session -> {
                    var entity = new EntityWithTimezones(PERSISTED_ZONED_DATE_TIME, PERSISTED_OFFSET_DATE_TIME,
                            PERSISTED_OFFSET_TIME);
                    return session.persist(entity).replaceWith(() -> entity.id);
                })
                        .chain(id -> sessionFactory.withTransaction(session -> session.find(EntityWithTimezones.class, id))),
                entity -> {
                    SoftAssertions.assertSoftly(assertions -> {
                        assertions.assertThat(entity).extracting("zonedDateTime").isEqualTo(expectedZonedDateTime);
                        assertions.assertThat(entity).extracting("offsetDateTime").isEqualTo(expectedOffsetDateTime);
                        assertions.assertThat(entity).extracting("offsetTime").isEqualTo(expectedOffsetTime);
                    });
                });
    }
}
