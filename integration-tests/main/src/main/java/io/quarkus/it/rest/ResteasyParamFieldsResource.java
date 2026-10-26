package io.quarkus.it.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/param-fields/{pathField}/{pathProperty}")
@RequestScoped
public class ResteasyParamFieldsResource {

    // make sure these don't break the build when fields
    @org.jboss.resteasy.annotations.jaxrs.PathParam
    String pathField;
    @org.jboss.resteasy.annotations.jaxrs.FormParam
    String formField;
    @org.jboss.resteasy.annotations.jaxrs.CookieParam
    String cookieField;
    @org.jboss.resteasy.annotations.jaxrs.HeaderParam
    String headerField;
    @org.jboss.resteasy.annotations.jaxrs.MatrixParam
    String matrixField;
    @org.jboss.resteasy.annotations.jaxrs.QueryParam
    String queryField;

    // make sure these don't break the build when properties
    public String getPathProperty() {
        return null;
    }

    @org.jboss.resteasy.annotations.jaxrs.PathParam
    public void setPathProperty(String p) {
    }

    public String getFormProperty() {
        return null;
    }

    @org.jboss.resteasy.annotations.jaxrs.FormParam
    public void setFormProperty(String p) {
    }

    public String getCookieProperty() {
        return null;
    }

    @org.jboss.resteasy.annotations.jaxrs.CookieParam
    public void setCookieProperty(String p) {
    }

    public String getHeaderProperty() {
        return null;
    }

    @org.jboss.resteasy.annotations.jaxrs.HeaderParam
    public void setHeaderProperty(String p) {
    }

    public String getMatrixProperty() {
        return null;
    }

    @org.jboss.resteasy.annotations.jaxrs.MatrixParam
    public void setMatrixProperty(String p) {
    }

    public String getQueryProperty() {
        return null;
    }

    @org.jboss.resteasy.annotations.jaxrs.QueryParam
    public void setQueryProperty(String p) {
    }

    @GET
    public String get() {
        return pathField + ":" + queryField;
    }
}
