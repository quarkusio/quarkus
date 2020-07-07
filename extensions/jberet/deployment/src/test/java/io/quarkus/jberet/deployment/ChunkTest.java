
package io.quarkus.jberet.deployment;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ChunkTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PeopleReader.class, PeopleWriter.class, PeopleProcessor.class,
                            PeopleDatabase.class, Person.class)
                    .addAsResource("people.txt")
                    .addAsManifestResource("chunk.xml", "batch-jobs/chunk.xml"));

    @Inject
    PeopleDatabase database;

    @Test
    public void runChunkJob() throws TimeoutException {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        long executionId = jobOperator.start("chunk", new Properties());
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });

        // verify that the job produced something in the 'database'
        List<Person> peopleList = database.getPeopleList();
        assertEquals("joe", peopleList.get(0).getUsername());
        assertNotNull(peopleList.get(0).getPassword());
        assertEquals("david", peopleList.get(1).getUsername());
        assertNotNull(peopleList.get(1).getPassword());
        assertEquals("mary", peopleList.get(2).getUsername());
        assertNotNull(peopleList.get(2).getPassword());
    }

    @Named
    @Dependent
    public static class PeopleReader implements ItemReader {
        private BufferedReader reader;

        @Override
        public void open(Serializable checkpoint) {
            // no checkpointing support in this basic example
            InputStream inputStream = this.getClass().getResourceAsStream("/people.txt");
            reader = new BufferedReader(new InputStreamReader(inputStream));
        }

        @Override
        public void close() {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object readItem() throws IOException {
            String name = reader.readLine();
            if (name != null) {
                return new Person(name);
            } else {
                // reached the end of file
                return null;
            }
        }

        @Override
        public Serializable checkpointInfo() {
            // no checkpointing support in this basic example
            return null;
        }
    }

    @Named
    @Dependent
    public static class PeopleProcessor implements ItemProcessor {
        @Override
        public Object processItem(Object item) {
            Person person = (Person) item;
            person.setPassword(UUID.randomUUID().toString());
            return person;
        }

    }

    @Dependent
    @Named
    public static class PeopleWriter implements ItemWriter {
        @Inject
        PeopleDatabase peopleDatabase;

        @Override
        public void open(Serializable checkpoint) {
        }

        @Override
        public void close() {
        }

        @Override
        public void writeItems(List<Object> items) {
            for (Object person : items) {
                peopleDatabase.addPerson((Person) person);
            }
        }

        @Override
        public Serializable checkpointInfo() {
            return null;
        }

    }

    @ApplicationScoped
    public static class PeopleDatabase {
        private final List<Person> peopleList = new CopyOnWriteArrayList<>();

        public void addPerson(Person person) {
            peopleList.add(person);
        }

        public List<Person> getPeopleList() {
            return peopleList;
        }
    }

    public static class Person {

        private String username;
        private String password;

        public Person(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
