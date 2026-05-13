package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.smallrye.common.annotation.NonBlocking;

@Path("/unsupported")
@NonBlocking
public class UnsupportedAnnotationResource {

    @ServerExceptionMapper
    public Response handleParseException(WebApplicationException e) {
        var cause = e.getCause() == null ? e : e.getCause();
        return Response.status(Response.Status.BAD_REQUEST).entity(cause.getMessage()).build();
    }

    // --- @JsonAnyGetter + @JsonAnySetter ---

    @GET
    @Path("/any-getter")
    public AnyGetterBean getAnyGetter() {
        AnyGetterBean bean = new AnyGetterBean();
        bean.setName("test");
        bean.addProperty("color", "red");
        bean.addProperty("size", "large");
        return bean;
    }

    @POST
    @Path("/any-getter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String echoAnyGetter(AnyGetterBean bean) {
        return "{\"name\":\"" + bean.getName()
                + "\",\"props_size\":" + bean.getProperties().size() + "}";
    }

    // --- @JsonAutoDetect ---

    @GET
    @Path("/auto-detect")
    public AutoDetectBean getAutoDetect() {
        return new AutoDetectBean("hello", 42);
    }

    // --- @JsonManagedReference + @JsonBackReference ---

    @GET
    @Path("/managed-reference")
    public ManagedReferenceParent getManagedReference() {
        ManagedReferenceParent parent = new ManagedReferenceParent();
        parent.setParentName("parent");
        ManagedReferenceChild child = new ManagedReferenceChild();
        child.setChildName("child");
        child.setParent(parent);
        parent.setChild(child);
        return parent;
    }

    // --- @JsonFormat ---

    @GET
    @Path("/format")
    public FormatBean getFormat() {
        FormatBean bean = new FormatBean();
        bean.setName("shape-test");
        bean.setShape(FormatShape.SQUARE);
        return bean;
    }

    @POST
    @Path("/format")
    @Consumes(MediaType.APPLICATION_JSON)
    public FormatBean echoFormat(FormatBean bean) {
        return bean;
    }

    // --- @JsonGetter + @JsonSetter ---

    @GET
    @Path("/getter-setter")
    public GetterSetterBean getGetterSetter() {
        GetterSetterBean bean = new GetterSetterBean();
        bean.setName("test");
        bean.setCount(5);
        return bean;
    }

    @POST
    @Path("/getter-setter")
    @Consumes(MediaType.APPLICATION_JSON)
    public GetterSetterBean echoGetterSetter(GetterSetterBean bean) {
        return bean;
    }

    // --- @JsonIgnoreType ---

    @GET
    @Path("/ignore-type")
    public IgnoreTypeBean getIgnoreType() {
        IgnoreTypeBean bean = new IgnoreTypeBean();
        bean.setName("visible");
        bean.setMetadata(new IgnoredType("secret-data"));
        return bean;
    }

    // --- @JsonInclude ---

    @GET
    @Path("/include-all-set")
    public IncludeBean getIncludeAllSet() {
        IncludeBean bean = new IncludeBean();
        bean.setName("test");
        bean.setNullableField("present");
        bean.setEmptyField("not-empty");
        return bean;
    }

    @GET
    @Path("/include-nulls")
    public IncludeBean getIncludeNulls() {
        IncludeBean bean = new IncludeBean();
        bean.setName("test");
        bean.setNullableField(null);
        bean.setEmptyField("");
        return bean;
    }

    // --- @JsonPropertyOrder ---

    @GET
    @Path("/property-order")
    public PropertyOrderBean getPropertyOrder() {
        PropertyOrderBean bean = new PropertyOrderBean();
        bean.setAlpha("a");
        bean.setMiddle("m");
        bean.setZebra("z");
        return bean;
    }

    // --- @JsonRawValue ---

    @GET
    @Path("/raw-value")
    public RawValueBean getRawValue() {
        RawValueBean bean = new RawValueBean();
        bean.setName("test");
        bean.setRawJson("{\"nested\":\"value\",\"count\":1}");
        return bean;
    }
}
