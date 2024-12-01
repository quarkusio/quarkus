package io.quarkus.smallrye.faulttolerance.test.config;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@Dependent
public class CircuitBreakerConfigBean {
    @CircuitBreaker(requestVolumeThreshold = 2, failOn = TestConfigExceptionB.class)
    public void failOn() {
        throw new TestConfigExceptionA();
    }

    @CircuitBreaker(requestVolumeThreshold = 2)
    public void skipOn() {
        throw new TestConfigExceptionA();
    }

    @CircuitBreaker(requestVolumeThreshold = 2, delay = 20, delayUnit = ChronoUnit.MICROS)
    public void delay(boolean fail) {
        if (fail) {
            throw new TestConfigExceptionA();
        }
    }

    @CircuitBreaker(requestVolumeThreshold = 2)
    public void requestVolumeThreshold() {
        throw new TestConfigExceptionA();
    }

    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 1.0)
    public void failureRatio(boolean fail) {
        if (fail) {
            throw new TestConfigExceptionA();
        }
    }

    @CircuitBreaker(requestVolumeThreshold = 10, successThreshold = 4, delay = 1000)
    public void successThreshold(boolean fail) {
        if (fail) {
            throw new TestConfigExceptionA();
        }
    }
}
