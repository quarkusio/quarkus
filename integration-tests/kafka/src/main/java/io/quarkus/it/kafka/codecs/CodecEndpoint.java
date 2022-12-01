package io.quarkus.it.kafka.codecs;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;

/**
 * Pet: Serialized and de-serialized with custom codec, topic: pets
 * Person: Serialized with Json-b serializer and de-serialized with an extension of the Json-b deserializer, topic: persons
 * Movie: Serialized with the Jackson serializer and de-serialized with an extension of the Jackson
 * deserializer (ObjectMapperDeserializer), topic: movies
 */
@Path("/codecs")
public class CodecEndpoint {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bs;

    public Producer<String, Pet> createPetProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "pet");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, PetCodec.class.getName());
        return new KafkaProducer<>(props);
    }

    public Producer<String, Person> createPersonProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "person");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonbSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    public Producer<String, List<Person>> createPersonListProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "person-list");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonbSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    public Producer<String, Movie> createMovieProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "movie");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ObjectMapperSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    public Producer<String, List<Movie>> createMovieListProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "movie-list");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ObjectMapperSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    public KafkaConsumer<String, Pet> createPetConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "pet");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PetCodec.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, Pet> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("pets"));
        return consumer;
    }

    public KafkaConsumer<String, Person> createPersonConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "person");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PersonDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, Person> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("persons"));
        return consumer;
    }

    public KafkaConsumer<String, List<Person>> createPersonListConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "person-list");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, PersonListDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, List<Person>> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("person-list"));
        return consumer;
    }

    public KafkaConsumer<String, Movie> createMovieConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "movie");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MovieDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, Movie> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("movies"));
        return consumer;
    }

    public KafkaConsumer<String, List<Movie>> createMovieListConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "movie-list");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, MovieListDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, List<Movie>> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("movie-list"));
        return consumer;
    }

    private Producer<String, Pet> petProducer;
    private Producer<String, Person> personProducer;
    private Consumer<String, Pet> petConsumer;
    private Consumer<String, Person> personConsumer;
    private KafkaConsumer<String, Movie> movieConsumer;
    private Producer<String, Movie> movieProducer;
    private Producer<String, List<Person>> personListProducer;
    private Consumer<String, List<Person>> personListConsumer;
    private Producer<String, List<Movie>> movieListProducer;
    private Consumer<String, List<Movie>> movieListConsumer;

    @PostConstruct
    public void create() {
        petProducer = createPetProducer();
        petConsumer = createPetConsumer();
        personProducer = createPersonProducer();
        personConsumer = createPersonConsumer();
        movieProducer = createMovieProducer();
        movieConsumer = createMovieConsumer();
        personListProducer = createPersonListProducer();
        personListConsumer = createPersonListConsumer();
        movieListProducer = createMovieListProducer();
        movieListConsumer = createMovieListConsumer();
    }

    @POST
    @Path("/pets")
    public void post(Pet pet) {
        petProducer.send(new ProducerRecord<>("pets", pet));
        petProducer.flush();
    }

    @POST
    @Path("/persons")
    public void post(Person person) {
        personProducer.send(new ProducerRecord<>("persons", person));
        personProducer.flush();
    }

    @POST
    @Path("/person-list")
    public void postListOfPersons(List<Person> persons) {
        personListProducer.send(new ProducerRecord<>("person-list", persons));
        personListProducer.flush();
    }

    @POST
    @Path("/movies")
    public void post(Movie movie) {
        movieProducer.send(new ProducerRecord<>("movies", movie));
        movieProducer.flush();
    }

    @POST
    @Path("/movie-list")
    public void postListOfMovies(List<Movie> movies) {
        movieListProducer.send(new ProducerRecord<>("movie-list", movies));
        movieListProducer.flush();
    }

    @GET
    @Path("/pets")
    public Pet getPet() {
        final ConsumerRecords<String, Pet> records = petConsumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

    @GET
    @Path("/persons")
    public Person getPerson() {
        final ConsumerRecords<String, Person> records = personConsumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

    @GET
    @Path("/person-list")
    public List<Person> listPersons() {
        final ConsumerRecords<String, List<Person>> records = personListConsumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

    @GET
    @Path("/movies")
    public Movie getMovie() {
        final ConsumerRecords<String, Movie> records = movieConsumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

    @GET
    @Path("/movie-list")
    public List<Movie> listMovie() {
        final ConsumerRecords<String, List<Movie>> records = movieListConsumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }
}
