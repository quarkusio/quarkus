package org.acme;

import org.acme.shared.BigBean;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class App {

    @Inject
    BigBean bean;

    @GET
    public String get() {
        return bean.getName();
    }
}
