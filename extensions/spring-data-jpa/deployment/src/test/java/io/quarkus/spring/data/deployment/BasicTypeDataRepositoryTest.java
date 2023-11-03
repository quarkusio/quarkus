package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.assertj.core.data.Percentage;
import org.hibernate.Hibernate;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import io.quarkus.test.QuarkusUnitTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicTypeDataRepositoryTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(WithDoubleValue.class, BasicTypeData.class, BasicTypeDataRepository.class))
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

    @Test
    @Order(5)
    @Transactional
    public void testMapInterfacesUsingList() {
        List<WithDoubleValue> list = repo.findAllDoubleValuesToList();
        Set<Double> actual = list.stream().map(WithDoubleValue::getDoubleValue).collect(Collectors.toSet());
        assertThat(actual).isNotEmpty().contains(Math.PI);
    }

    @Test
    @Order(6)
    @Transactional
    public void testMapInterfacesUsingPage() {
        // Only 1 element in db, so it should return 1 page of 1 size
        Page<WithDoubleValue> page = repo.findAllDoubleValuesToPage(PageRequest.of(0, 1));
        Set<Double> actual = page.stream().map(WithDoubleValue::getDoubleValue).collect(Collectors.toSet());
        assertThat(actual).isNotEmpty().contains(Math.PI);
        assertEquals(1, page.getNumberOfElements());
        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getTotalPages());

        // next page should return zero elements
        Page<WithDoubleValue> nextPage = repo.findAllDoubleValuesToPage(PageRequest.of(1, 1));
        assertEquals(0, nextPage.getNumberOfElements());
        assertEquals(1, nextPage.getTotalElements());
        assertEquals(1, nextPage.getTotalPages());
    }

    @Test
    @Order(7)
    @Transactional
    public void testMapInterfacesUsingSlice() {
        // Only 1 element in db, so it should return 1 page of 1 size
        Slice<WithDoubleValue> slice = repo.findAllDoubleValuesToSlice(PageRequest.of(0, 1));
        Set<Double> actual = slice.stream().map(WithDoubleValue::getDoubleValue).collect(Collectors.toSet());
        assertThat(actual).isNotEmpty().contains(Math.PI);
        assertEquals(1, slice.getNumberOfElements());

        // next page should return zero elements
        Slice<WithDoubleValue> nextPage = repo.findAllDoubleValuesToSlice(PageRequest.of(1, 1));
        assertEquals(0, nextPage.getNumberOfElements());
    }

    @Test
    @Order(8)
    @Transactional
    public void testListOrderByNullHandling() throws MalformedURLException {
        // Insert row with null in url
        UUID uuidForTheNullUrlRecord = UUID.randomUUID();
        BasicTypeData item = populateData(new BasicTypeData());
        item.setUuid(uuidForTheNullUrlRecord);
        item.setUrl(null);
        repo.save(item);
        // At this moment, there should be at least two records, one inserted in the first test,
        // and the second with the "url" column to null.

        // Check Nulls first
        List<BasicTypeData> list = repo.findAll(Sort.by(Sort.Order.by("url").nullsFirst()));
        assertEquals(uuidForTheNullUrlRecord, list.get(0).getUuid());

        // Check Nulls last
        list = repo.findAll(Sort.by(Sort.Order.by("url").nullsLast()));
        assertEquals(uuidForTheNullUrlRecord, list.get(list.size() - 1).getUuid());
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
