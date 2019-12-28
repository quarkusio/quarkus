package io.quarkus.it.spring;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import io.quarkus.it.spring.AppConfiguration.CustomPrototypeBean;
import io.quarkus.it.spring.AppConfiguration.NamedBean;

@Path("/")
public class InjectedSpringBeansResource {

    @Inject
    GreeterBean greeterBean;
    @Inject
    RequestBean requestBean;
    @Inject
    SessionBean sessionBean;
    @Inject
    CustomPrototypeBean anotherRequestBean;
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
}
