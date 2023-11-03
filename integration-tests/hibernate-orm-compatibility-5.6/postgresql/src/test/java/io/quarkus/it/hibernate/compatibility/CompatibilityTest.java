package io.quarkus.it.hibernate.compatibility;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CompatibilityTest {

    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#implicit-identifier-sequence-and-table-name
    @Test
    @Order(1)
    public void sequence_defaultGenerator() {
        var entity = new MyEntity();
        MyEntity createdEntity = given()
                .body(entity).contentType("application/json")
                .when().post("/compatibility/").then()
                .assertThat().statusCode(is(Status.OK.getStatusCode()))
                .extract().as(MyEntity.class);
        assertThat(createdEntity.id).isEqualTo(3);
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#id-sequence-defaults
    @Test
    @Order(2)
    public void sequence_genericGenerator_defaultAllocation() {
        var entity = new MyEntityWithGenericGeneratorAndDefaultAllocationSize();
        MyEntityWithGenericGeneratorAndDefaultAllocationSize createdEntity = given()
                .body(entity).contentType("application/json")
                .when().post("/compatibility/genericgenerator").then()
                .assertThat().statusCode(is(Status.OK.getStatusCode()))
                .extract().as(MyEntityWithGenericGeneratorAndDefaultAllocationSize.class);
        assertThat(createdEntity.id).isEqualTo(3);
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#id-sequence-defaults
    @Test
    @Order(3)
    public void sequence_sequenceGenerator_defaultAllocation() {
        var entity = new MyEntityWithSequenceGeneratorAndDefaultAllocationSize();
        MyEntityWithSequenceGeneratorAndDefaultAllocationSize createdEntity = given()
                .body(entity).contentType("application/json")
                .when().post("/compatibility/sequencegenerator").then()
                .assertThat().statusCode(is(Status.OK.getStatusCode()))
                .extract().as(MyEntityWithSequenceGeneratorAndDefaultAllocationSize.class);
        // Sequence generators defined through @SequenceGenerator have always defaulted to an allocation size of 50.
        // Since we've created 2 entities in Hibernate 5, we should be starting the second pool here, starting at 52.
        // BUT! Quarkus 3 changes the default ID optimizer to pooled-lo,
        // which means we'll interpret the value returned by the sequence (101) as the *start* of the pool
        // instead of the *end* of the pool. That's an expected change and should be harmless.
        assertThat(createdEntity.id).isEqualTo(101L);
    }

    // Just check that persisting with the old schema and new application does not throw any exception
    @Test
    @Order(4) // So that we can assert the generated ID in sequence*()
    public void persistUsingOldSchema() {
        var entity = new MyEntity();
        entity.duration = Duration.of(59, ChronoUnit.SECONDS);
        entity.uuid = UUID.fromString("f49c6ba8-8d7f-417a-a255-d594dddf729f");
        entity.instant = Instant.parse("2018-01-01T10:58:30.00Z");
        entity.intArray = new int[] { 0, 1, 42 };
        entity.offsetTime = LocalTime.of(12, 58, 30, 0)
                .atOffset(ZoneOffset.ofHours(2));
        entity.offsetDateTime = LocalDateTime.of(2018, 1, 1, 12, 58, 30, 0)
                .atOffset(ZoneOffset.ofHours(2));
        entity.zonedDateTime = LocalDateTime.of(2018, 1, 1, 12, 58, 30, 0)
                .atZone(ZoneId.of("Africa/Cairo" /* UTC+2 */));
        entity.stringList = new ArrayList<>(List.of("one", "two"));
        entity.myEnum = MyEnum.VALUE2;
        given()
                .body(entity).contentType("application/json")
                .when().post("/compatibility/").then()
                .assertThat().statusCode(is(Status.OK.getStatusCode()));
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#duration-mapping-changes
    @Test
    public void duration() {
        assertThat(findOld().duration).isEqualTo(Duration.of(59, ChronoUnit.SECONDS));
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#uuid-mapping-changes
    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#uuid-mapping-changes-on-mariadb
    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#uuid-mapping-changes-on-sql-server
    @Test
    public void uuid() {
        assertThat(findOld().uuid).isEqualTo(UUID.fromString("f49c6ba8-8d7f-417a-a255-d594dddf729f"));
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#instant-mapping-changes
    @Test
    public void instant() {
        assertThat(findOld().instant).isEqualTo(Instant.parse("2018-01-01T10:58:30.00Z"));
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#ddl-offset-time
    @Test
    public void offsetTime() {
        assertThat(findOld().offsetTime)
                .isEqualTo(LocalTime.of(12, 58, 30, 0)
                        // Hibernate ORM 5 used to normalize these values to the JVM TZ
                        .atOffset(ZoneId.systemDefault().getRules().getOffset(Instant.now())));
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#timezone-and-offset-storage
    @Test
    public void offsetDateTime() {
        assertThat(findOld().offsetDateTime)
                .isEqualTo(LocalDateTime.of(2018, 1, 1, 12, 58, 30, 0)
                        .atOffset(ZoneOffset.ofHours(2))
                        // Hibernate ORM 5 used to normalize these values to the JVM TZ
                        .atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime());
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#timezone-and-offset-storage
    @Test
    public void zonedDateTime() {
        assertThat(findOld().zonedDateTime)
                .isEqualTo(LocalDateTime.of(2018, 1, 1, 12, 58, 30, 0)
                        .atZone(ZoneId.of("Africa/Cairo" /* UTC+2 */))
                        // Hibernate ORM 5 used to normalize these values to the JVM TZ
                        .withZoneSameInstant(ZoneId.systemDefault()));
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#basic-arraycollection-mapping
    // Note this is not fixed automatically by Quarkus and requires new annotations on the entity (see entity class).
    @Test
    public void array() {
        assertThat(findOld().intArray).isEqualTo(new int[] { 0, 1, 42 });
    }

    // https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#basic-arraycollection-mapping
    // Note this is not fixed automatically by Quarkus and requires new annotations on the entity (see entity class).
    @Test
    public void list() {
        assertThat(findOld().stringList).isEqualTo(new ArrayList<>(List.of("one", "two")));
    }

    @Test
    // https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#enum-mapping-changes
    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#datatype-for-enums
    public void enum_() {
        assertThat(findOld().myEnum).isEqualTo(MyEnum.VALUE2);
    }

    private static MyEntity findOld() {
        return find(1L);
    }

    private static MyEntity find(long id) {
        return given().when().get("/compatibility/{id}", id).then()
                .assertThat().statusCode(is(Status.OK.getStatusCode()))
                .extract().as(MyEntity.class);
    }

}
