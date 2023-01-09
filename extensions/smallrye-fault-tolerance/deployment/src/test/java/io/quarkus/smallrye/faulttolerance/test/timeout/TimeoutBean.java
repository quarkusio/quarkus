package io.quarkus.smallrye.faulttolerance.test.timeout;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class TimeoutBean {
    @Timeout(10)
    public void hello() throws InterruptedException {
        Thread.sleep(250);
    }
}
