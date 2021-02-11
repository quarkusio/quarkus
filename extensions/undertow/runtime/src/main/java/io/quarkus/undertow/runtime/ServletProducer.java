package io.quarkus.undertow.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

    @Produces
    @RequestScoped
    HttpSession session() {
        return request().getSession();
    }
}
