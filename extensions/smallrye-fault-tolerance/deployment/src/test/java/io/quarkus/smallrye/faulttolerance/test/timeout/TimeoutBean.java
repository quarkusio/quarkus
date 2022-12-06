package io.quarkus.smallrye.faulttolerance.test.timeout;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class TimeoutBean {

    @Timeout(50)
    public void timeout() throws InterruptedException {
        Thread.sleep(100);
    }
}
