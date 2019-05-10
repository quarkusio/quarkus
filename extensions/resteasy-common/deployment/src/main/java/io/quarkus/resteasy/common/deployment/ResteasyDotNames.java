package io.quarkus.resteasy.common.deployment;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.jboss.jandex.DotName;

public final class ResteasyDotNames {

    public static final DotName CONSUMES = DotName.createSimple(Consumes.class.getName());
    public static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());
    public static final DotName PROVIDER = DotName.createSimple(Provider.class.getName());
    public static final DotName GET = DotName.createSimple(javax.ws.rs.GET.class.getName());
    public static final DotName HEAD = DotName.createSimple(javax.ws.rs.HEAD.class.getName());
    public static final DotName DELETE = DotName.createSimple(javax.ws.rs.DELETE.class.getName());
    public static final DotName OPTIONS = DotName.createSimple(javax.ws.rs.OPTIONS.class.getName());
    public static final DotName PATCH = DotName.createSimple(javax.ws.rs.PATCH.class.getName());
    public static final DotName POST = DotName.createSimple(javax.ws.rs.POST.class.getName());
    public static final DotName PUT = DotName.createSimple(javax.ws.rs.PUT.class.getName());
    public static final DotName APPLICATION_PATH = DotName.createSimple(ApplicationPath.class.getName());
    public static final DotName PATH = DotName.createSimple(Path.class.getName());
    public static final DotName DYNAMIC_FEATURE = DotName.createSimple(DynamicFeature.class.getName());
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName RESTEASY_QUERY_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.QueryParam.class.getName());
    public static final DotName RESTEASY_FORM_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.FormParam.class.getName());
    public static final DotName RESTEASY_COOKIE_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.CookieParam.class.getName());
    public static final DotName RESTEASY_PATH_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.PathParam.class.getName());
    public static final DotName RESTEASY_HEADER_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.HeaderParam.class.getName());
    public static final DotName RESTEASY_MATRIX_PARAM = DotName
            .createSimple(org.jboss.resteasy.annotations.jaxrs.MatrixParam.class.getName());

}
