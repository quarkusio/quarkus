package io.quarkus.it.spring;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.it.spring.AppConfiguration.CustomPrototypeBean;
import io.quarkus.it.spring.AppConfiguration.NamedBean;
import io.quarkus.it.spring.AppConfiguration.PrototypeBean;

@Path("/")
public class InjectedSpringBeansResource {

    @Inject
    GreeterBean greeterBean;
    @Inject
    RequestBean requestBean;
    @Inject
    SessionBean sessionBean;
    @Inject
    CustomPrototypeBean customPrototypeBean;
    @Inject
    PrototypeBean prototypeBean;
    @Inject
    PrototypeBean anotherPrototypeBean;
    @Inject
    NamedBean namedBean;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return greeterBean.greet("world");
    }

    @GET
    @Path("request")
    @Produces(MediaType.TEXT_PLAIN)
    public int getRequestValue() {
        return requestBean.getValue();
    }

    @GET
    @Path("session")
    @Produces(MediaType.TEXT_PLAIN)
    public int getSessionValue() {
        return sessionBean.getValue();
    }

    @POST
    @Path("invalidate")
    public void invalidate(final @Context HttpServletRequest req) {
        req.getSession().invalidate();
    }

    @GET
    @Path("prototype")
    public String prototype() {
        return prototypeBean.index + "/" + anotherPrototypeBean.index;
    }
}
