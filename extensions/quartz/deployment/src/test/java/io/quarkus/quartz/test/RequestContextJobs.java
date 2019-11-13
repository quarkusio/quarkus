package io.quarkus.quartz.test;

import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;

public class RequestContextJobs {

    static final CountDownLatch LATCH = new CountDownLatch(1);

    @Inject
    RequestFoo foo;

    @Scheduled(every = "1s")
    void checkEverySecond() {
        foo.getName();
        LATCH.countDown();
    }

    @RequestScoped
    static class RequestFoo {

        private String name;

        @PostConstruct
        void init() {
            name = "oof";
        }

        public String getName() {
            return name;
        }

    }

}
