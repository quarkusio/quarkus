package io.quarkus.forkjoin;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class ProductionModeTestsEndpoint implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {

        //check system properties set
        if (!"org.jboss.logmanager.LogManager".equals(System.getProperty("java.util.logging.manager"))) {
            System.out.println("java.util.logging.manager not set");
            System.out.println("FAIL");
            return 1;
        }
        if (!"io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory"
                .equals(System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory"))) {
            System.out.println("java.util.concurrent.ForkJoinPool.common.threadFactory not set");
            System.out.println("FAIL");
            return 1;
        }
        if (!ForkJoinPoolAssertions.isEachFJThreadUsingQuarkusClassloader()) {
            System.out.println("fork join assertions failed");
            System.out.println("FAIL");
            return 1;
        }
        System.out.println("PASS");
        return 0;
    }
}
