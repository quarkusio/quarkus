package io.quarkus.resteasy.reactive.server.servlet.runtime;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;

import org.jboss.resteasy.reactive.server.core.Deployment;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.util.ImmediateInstanceFactory;

@Recorder
public class ResteasyReactiveServletRecorder {

    public InstanceFactory<Servlet> servlet(RuntimeValue<Deployment> deployment) {
        return new ImmediateInstanceFactory<>(new ResteasyReactiveServlet(deployment.getValue()));
    }

    public InstanceFactory<Filter> filter(RuntimeValue<Deployment> deployment) {
        return new ImmediateInstanceFactory<>(new ResteasyReactiveFilter(deployment.getValue()));
    }

}
