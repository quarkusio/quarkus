package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.assertj.core.data.Percentage;
import org.hibernate.Hibernate;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicTypeDataRepositoryTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BasicTypeData.class, BasicTypeDataRepository.class))
            .withConfigurationResource("application.properties");

    private static final UUID uuid = UUID.randomUUID();
    private static final String QUARKUS_URL = "https://quarkus.io/guides/spring-data-jpa";
    private static final String DURATION = "PT828H19M54.656S";
    private static final String TIME_ZONE = "CST";

    @Inject
    BasicTypeDataRepository repo;

    @Test
    @Order(1)
    @Transactional
    public void testInsert() throws Exception {
        BasicTypeData item = populateData(new BasicTypeData());
        repo.save(item);
    }

    @Test
    @Order(2)
    @Transactional
    public void testDoubleByURL() throws Exception {
        Double price = repo.doubleByURL(new URL(QUARKUS_URL));
        assertThat(price).isCloseTo(Math.PI, Percentage.withPercentage(1));
    }

    @Test
    @Order(3)
    @Transactional
    public void testDurationByUUID() {
        Duration duration = repo.durationByUUID(uuid);
        assertThat(duration).isEqualTo(Duration.parse(DURATION));
    }

    @Test
    @Order(4)
    @Transactional
    public void testTimeZonesByLocale() {
        final Set<TimeZone> timeZones = repo.timeZonesByLocale(Locale.TRADITIONAL_CHINESE);
        assertThat(timeZones).isNotEmpty().contains(TimeZone.getTimeZone("CST"));
    }

    private BasicTypeData populateData(BasicTypeData basicTypeData) throws MalformedURLException {
        basicTypeData.setDoubleValue(Math.PI);
        basicTypeData.setBigDecimalValue(BigDecimal.valueOf(Math.PI * 2.0));
        basicTypeData.setLocale(Locale.TRADITIONAL_CHINESE);
        basicTypeData.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
        basicTypeData.setUrl(new URL(QUARKUS_URL));
        basicTypeData.setClazz(Hibernate.class);
        basicTypeData.setUuid(uuid);
        basicTypeData.setDuration(Duration.parse(DURATION));

        return basicTypeData;
    }
}
