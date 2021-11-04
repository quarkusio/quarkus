import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SimpleScheduledMethodDefaultPackageTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleJobsDefaultPackage.class)
                    .addAsResource(new StringAsset("simpleJobs.cron=0/1 * * * * ?\nsimpleJobs.every=1s"),
                            "application.properties"));

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        for (CountDownLatch latch : SimpleJobsDefaultPackage.LATCHES.values()) {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
    }

}
