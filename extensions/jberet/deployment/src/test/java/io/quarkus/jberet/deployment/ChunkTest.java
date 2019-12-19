
package io.quarkus.jberet.deployment;

import io.quarkus.jberet.deployment.helpers.JobHelper;
import io.quarkus.jberet.deployment.people.PeopleDatabase;
import io.quarkus.jberet.deployment.people.PeopleProcessor;
import io.quarkus.jberet.deployment.people.PeopleReader;
import io.quarkus.jberet.deployment.people.PeopleWriter;
import io.quarkus.jberet.deployment.people.Person;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChunkTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PeopleReader.class, PeopleWriter.class, PeopleProcessor.class,
                            PeopleDatabase.class)
                    .addAsResource("people.txt")
                    .addAsManifestResource("chunk.xml",
                            "batch-jobs/chunk.xml"));

    @Inject
    PeopleDatabase database;

    @Test
    public void runChunkJob() throws TimeoutException {
        JobOperator operator = BatchRuntime.getJobOperator();

        // run the job
        long executionId = operator.start("chunk", null);
        assertEquals(BatchStatus.COMPLETED, JobHelper.waitForExecutionFinish(executionId));

        // verify that the job produced something in the 'database'
        List<Person> peopleList = database.getPeopleList();
        Assertions.assertEquals("joe", peopleList.get(0).getUsername());
        Assertions.assertNotNull(peopleList.get(0).getPassword());
        Assertions.assertEquals("david", peopleList.get(1).getUsername());
        Assertions.assertNotNull(peopleList.get(1).getPassword());
        Assertions.assertEquals("mary", peopleList.get(2).getUsername());
        Assertions.assertNotNull(peopleList.get(2).getPassword());
    }

}
