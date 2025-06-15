package io.quarkus.hibernate.orm.mapping.timezone;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.inject.Inject;

import org.assertj.core.api.SoftAssertions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import io.quarkus.narayana.jta.QuarkusTransaction;

public class AbstractTimezoneDefaultStorageTest {

    private static final LocalDateTime LOCAL_DATE_TIME_TO_TEST = LocalDateTime.of(2017, Month.NOVEMBER, 6, 19, 19, 0);
    public static final ZonedDateTime PERSISTED_ZONED_DATE_TIME = LOCAL_DATE_TIME_TO_TEST
            .atZone(ZoneId.of("Africa/Cairo"));
    public static final OffsetDateTime PERSISTED_OFFSET_DATE_TIME = LOCAL_DATE_TIME_TO_TEST
            .atOffset(ZoneOffset.ofHours(3));
    public static final OffsetTime PERSISTED_OFFSET_TIME = LOCAL_DATE_TIME_TO_TEST.toLocalTime()
            .atOffset(ZoneOffset.ofHours(3));

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    protected long persistWithValuesToTest() {
        return QuarkusTransaction.requiringNew().call(() -> {
            var entity = new EntityWithTimezones(PERSISTED_ZONED_DATE_TIME, PERSISTED_OFFSET_DATE_TIME,
                    PERSISTED_OFFSET_TIME);
            session.persist(entity);
            return entity.id;
        });
    }

    protected void assertLoadedValues(long id, ZonedDateTime expectedZonedDateTime,
            OffsetDateTime expectedOffsetDateTime, OffsetTime expectedOffsetTime) {
        QuarkusTransaction.requiringNew().run(() -> {
            var entity = session.find(EntityWithTimezones.class, id);
            SoftAssertions.assertSoftly(assertions -> {
                assertions.assertThat(entity).extracting("zonedDateTime").isEqualTo(expectedZonedDateTime);
                assertions.assertThat(entity).extracting("offsetDateTime").isEqualTo(expectedOffsetDateTime);
                assertions.assertThat(entity).extracting("offsetTime").isEqualTo(expectedOffsetTime);
            });
        });
    }
}
