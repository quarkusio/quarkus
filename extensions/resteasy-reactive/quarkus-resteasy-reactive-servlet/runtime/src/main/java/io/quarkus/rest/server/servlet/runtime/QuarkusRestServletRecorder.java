package io.quarkus.rest.server.servlet.runtime;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.jboss.resteasy.reactive.server.core.Deployment;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.util.ImmediateInstanceFactory;

@Recorder
public class QuarkusRestServletRecorder {

    public InstanceFactory<Servlet> servlet(RuntimeValue<Deployment> deployment) {
        return new ImmediateInstanceFactory<>(new QuarkusRestServlet(deployment.getValue()));
    }

    public InstanceFactory<Filter> filter(RuntimeValue<Deployment> deployment) {
        return new ImmediateInstanceFactory<>(new QuarkusRestServletFilter(deployment.getValue()));
    }

}
