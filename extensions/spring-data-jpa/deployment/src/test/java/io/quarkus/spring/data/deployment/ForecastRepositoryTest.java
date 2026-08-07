package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.MonthDay;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Reproducer for https://github.com/quarkusio/quarkus/issues/51750.
 *
 * A {@code @Query} returning a type that is not part of the Jandex index used to fail the
 * build with {@code IllegalStateException: ... was not part of the Quarkus index}, because
 * the Spring Data JPA processor rejected any return type it could not find in the index.
 *
 * {@link MonthDay} reproduces this: it is a JDK class, so it is always on the classpath but
 * never part of the application's index, and unlike {@code java.time.Year} it is not one of
 * the types Hibernate ORM provides natively, so it is persisted through an
 * {@link jakarta.persistence.AttributeConverter}. The build must now succeed and leave the
 * type for Hibernate ORM to resolve.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ForecastRepositoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Forecast.class, ForecastRepository.class, MonthDayConverter.class))
            .withConfigurationResource("application.properties");

    private static final MonthDay MARCH_9 = MonthDay.of(3, 9);
    private static final MonthDay JUNE_1 = MonthDay.of(6, 1);
    private static final MonthDay DECEMBER_29 = MonthDay.of(12, 29);

    @Inject
    ForecastRepository repo;

    @Test
    @Order(1)
    @Transactional
    public void testInsert() {
        repo.save(new Forecast(MARCH_9, 100));
        repo.save(new Forecast(JUNE_1, 250));
        // Excluded by the query's WHERE clause
        repo.save(new Forecast(DECEMBER_29, 0));
    }

    @Test
    @Order(2)
    @Transactional
    public void testQueryReturningTypeOutsideTheIndex() {
        List<MonthDay> renewals = repo.findDistinctRenewalsWithNonZeroForecasts();

        assertThat(renewals).containsExactly(MARCH_9, JUNE_1);
    }
}
