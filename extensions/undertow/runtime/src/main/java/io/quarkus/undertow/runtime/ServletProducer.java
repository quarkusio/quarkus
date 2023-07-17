package io.quarkus.undertow.runtime;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
