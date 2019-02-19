package org.jboss.shamrock.undertow.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.servlet.handlers.ServletRequestContext;

@Singleton
public class ServletProducer {

    @Produces
    @RequestScoped
    HttpServletRequest request() {
        return (HttpServletRequest) ServletRequestContext.requireCurrent().getServletRequest();
    }

    @Produces
    @RequestScoped
    HttpServletResponse response() {
        return (HttpServletResponse) ServletRequestContext.requireCurrent().getServletResponse();
    }
}
