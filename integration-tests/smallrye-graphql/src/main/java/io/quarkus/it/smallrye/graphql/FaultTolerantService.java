package io.quarkus.it.smallrye.graphql;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.eclipse.microprofile.faulttolerance.Timeout;

@Singleton
public class FaultTolerantService {

    @Timeout(10)
    public void causeTimeout() {
        try {
            TimeUnit.MILLISECONDS.sleep(2500);
        } catch (InterruptedException e) {
        }
    }
}
