package io.quarkus.quartz.test;

import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class NoQuartzSchedulerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setExpectedException(UnsatisfiedResolutionException.class);

    @Inject
    org.quartz.Scheduler quartzScheduler;

    @Test
    public void test() {
    }
}
