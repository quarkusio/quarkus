package io.quarkus.it.kafka.streams;

import static org.hamcrest.Matchers.containsString;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTestResource(KafkaSSLTestResource.class)
@QuarkusTest
public class KafkaStreamsTest {

    private static void addSSL(Properties props) {
        File sslDir = new File("src/main/resources");
        File tsFile = new File(sslDir, "ks-truststore.p12");
        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, tsFile.getPath());
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L");
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
        props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
    }

    private static Producer<Integer, Customer> createCustomerProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaSSLTestResource.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "streams-test-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ObjectMapperSerializer.class.getName());
        addSSL(props);

        return new KafkaProducer<>(props);
    }

    private static Producer<Integer, Category> createCategoryProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaSSLTestResource.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "streams-test-category-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ObjectMapperSerializer.class.getName());
        addSSL(props);

        return new KafkaProducer<>(props);
    }

    private static KafkaConsumer<Integer, EnrichedCustomer> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaSSLTestResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "streams-test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EnrichedCustomerDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        addSSL(props);

        KafkaConsumer<Integer, EnrichedCustomer> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("streams-test-customers-processed"));
        return consumer;
    }

    @Test
    public void testKafkaStreams() throws Exception {
        testKafkaStreamsNotAliveAndNotReady();

        produceCustomers();

        Consumer<Integer, EnrichedCustomer> consumer = createConsumer();
        List<ConsumerRecord<Integer, EnrichedCustomer>> records = poll(consumer, 4);

        ConsumerRecord<Integer, EnrichedCustomer> record = records.get(0);
        Assertions.assertEquals(101, record.key());
        EnrichedCustomer customer = record.value();
        Assertions.assertEquals(101, customer.id);
        Assertions.assertEquals("Bob", customer.name);
        Assertions.assertEquals("B2B", customer.category.name);
        Assertions.assertEquals("business-to-business", customer.category.value);

        record = records.get(1);
        Assertions.assertEquals(102, record.key());
        customer = record.value();
        Assertions.assertEquals(102, customer.id);
        Assertions.assertEquals("Becky", customer.name);
        Assertions.assertEquals("B2C", customer.category.name);
        Assertions.assertEquals("business-to-customer", customer.category.value);

        record = records.get(2);
        Assertions.assertEquals(103, record.key());
        customer = record.value();
        Assertions.assertEquals(103, customer.id);
        Assertions.assertEquals("Bruce", customer.name);
        Assertions.assertEquals("B2B", customer.category.name);
        Assertions.assertEquals("business-to-business", customer.category.value);

        record = records.get(3);
        Assertions.assertEquals(104, record.key());
        customer = record.value();
        Assertions.assertEquals(104, customer.id);
        Assertions.assertEquals("Bert", customer.name);
        Assertions.assertEquals("B2B", customer.category.name);
        Assertions.assertEquals("business-to-business", customer.category.value);

        // test interactive query (getting latest result from state store)
        assertCategoryCount(1, 3);
        assertCategoryCount(2, 1);

        testKafkaStreamsAliveAndReady();
        RestAssured.when().get("/kafkastreams/state").then().body(CoreMatchers.is("RUNNING"));

        testMetricsPresent();

        // explicitly stopping the pipeline *before* the broker is shut down, as it
        // otherwise will time out
        RestAssured.post("/kafkastreams/stop");
    }

    private void testMetricsPresent() {
        // Look for kafka consumer metrics (add .log().all() to examine what they are
        RestAssured.when().get("/q/metrics").then()
                .statusCode(200)
                .body(containsString("kafka_stream_"));
    }

    public void testKafkaStreamsNotAliveAndNotReady() throws Exception {
        RestAssured.get("/q/health/ready").then()
                .statusCode(HttpStatus.SC_SERVICE_UNAVAILABLE)
                .body("checks[0].name", CoreMatchers.is("Kafka Streams topics health check"))
                .body("checks[0].status", CoreMatchers.is("DOWN"))
                .body("checks[0].data.missing_topics", CoreMatchers.is("streams-test-customers,streams-test-categories"));

        RestAssured.when().get("/q/health/live").then()
                .statusCode(HttpStatus.SC_SERVICE_UNAVAILABLE)
                .body("checks[0].name", CoreMatchers.is("Kafka Streams state health check"))
                .body("checks[0].status", CoreMatchers.is("DOWN"))
                .body("checks[0].data.state", CoreMatchers.is("CREATED"));

        RestAssured.when().get("/q/health").then()
                .statusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

    public void testKafkaStreamsAliveAndReady() throws Exception {
        RestAssured.get("/q/health/ready").then()
                .statusCode(HttpStatus.SC_OK)
                .body("checks[0].name", CoreMatchers.is("Kafka Streams topics health check"))
                .body("checks[0].status", CoreMatchers.is("UP"))
                .body("checks[0].data.available_topics", CoreMatchers.is("streams-test-categories,streams-test-customers"));

        RestAssured.when().get("/q/health/live").then()
                .statusCode(HttpStatus.SC_OK)
                .body("checks[0].name", CoreMatchers.is("Kafka Streams state health check"))
                .body("checks[0].status", CoreMatchers.is("UP"))
                .body("checks[0].data.state", CoreMatchers.is("RUNNING"));

        RestAssured.when().get("/q/health").then()
                .statusCode(HttpStatus.SC_OK);
    }

    private void produceCustomers() {
        Producer<Integer, Customer> producer = createCustomerProducer();

        Producer<Integer, Category> categoryProducer = createCategoryProducer();

        categoryProducer.send(new ProducerRecord<>("streams-test-categories", 1, new Category("B2B", "business-to-business")));
        categoryProducer.send(new ProducerRecord<>("streams-test-categories", 2, new Category("B2C", "business-to-customer")));

        producer.send(new ProducerRecord<>("streams-test-customers", 101, new Customer(101, "Bob", 1)));
        producer.send(new ProducerRecord<>("streams-test-customers", 102, new Customer(102, "Becky", 2)));
        producer.send(new ProducerRecord<>("streams-test-customers", 103, new Customer(103, "Bruce", 1)));
        producer.send(new ProducerRecord<>("streams-test-customers", 104, new Customer(104, "Bert", 1)));
    }

    private void assertCategoryCount(int categoryId, int expectedCount) throws Exception {
        int i = 0;
        Integer actual = null;

        // retrying for some time as the aggregation might not have finished yet
        while (i < 50 && !Integer.valueOf(expectedCount).equals(actual)) {
            actual = getCategoryCount(categoryId);
            Thread.sleep(100);
        }

        Assertions.assertEquals(expectedCount, actual);
    }

    private Integer getCategoryCount(int categoryId) {
        String result = RestAssured.when().get("/kafkastreams/category/" + categoryId).asString();
        if (result != null && !result.trim().isEmpty()) {
            return Integer.valueOf(result);
        }

        return null;
    }

    private List<ConsumerRecord<Integer, EnrichedCustomer>> poll(Consumer<Integer, EnrichedCustomer> consumer,
            int expectedRecordCount) {
        int fetched = 0;
        List<ConsumerRecord<Integer, EnrichedCustomer>> result = new ArrayList<>();
        while (fetched < expectedRecordCount) {
            ConsumerRecords<Integer, EnrichedCustomer> records = consumer.poll(Duration.ofMillis(20000));
            records.forEach(result::add);
            fetched = result.size();
        }

        return result;
    }

    public static class EnrichedCustomerDeserializer extends ObjectMapperDeserializer<EnrichedCustomer> {

        public EnrichedCustomerDeserializer() {
            super(EnrichedCustomer.class);
        }
    }
}
