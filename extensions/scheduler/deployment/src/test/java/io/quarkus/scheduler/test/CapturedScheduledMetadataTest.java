package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The schedules captured by the {@link SchedulerContext} service must carry the same values as the annotations,
 * defaults included.
 */
public class CapturedScheduledMetadataTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class));

    @Inject
    SchedulerContext context;

    @Test
    public void testCapturedSchedulesHaveTheAnnotationValues() throws Exception {
        ScheduledMethod method = context.getScheduledMethods().stream()
                .filter(m -> m.getMethodName().equals("run"))
                .findFirst()
                .orElseThrow();
        Scheduled[] expected = Jobs.class.getDeclaredMethod("run").getAnnotationsByType(Scheduled.class);
        List<Scheduled> actual = method.getSchedules();

        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            for (Method member : Scheduled.class.getDeclaredMethods()) {
                assertEquals(member.invoke(expected[i]), member.invoke(actual.get(i)),
                        "Member " + member.getName() + " of schedule " + i);
            }
            assertEquals(expected[i], actual.get(i));
            assertEquals(actual.get(i), expected[i]);
            assertEquals(expected[i].hashCode(), actual.get(i).hashCode());
        }
    }

    @ApplicationScoped
    static class Jobs {

        @Scheduled(identity = "explicit", every = "1h", delay = 5, delayUnit = TimeUnit.HOURS, delayed = "1h", overdueGracePeriod = "2h", concurrentExecution = ConcurrentExecution.SKIP, skipExecutionIf = Scheduled.ApplicationNotRunning.class, timeZone = "UTC", description = "explicit values", executionMaxDelay = "30m")
        @Scheduled(cron = "0 0 0 1 1 ?")
        void run() {
        }

    }

}
