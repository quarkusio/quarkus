package io.quarkus.smallrye.opentracing.deployment;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import io.opentracing.util.GlobalTracer;

@WebListener
public class TracerRegistrar implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        GlobalTracer.register(TracingTest.mockTracer);
    }
}
