package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.fasterxml.jackson.annotation.JsonView;

import io.smallrye.common.annotation.NonBlocking;

@Path("/generated")
@NonBlocking
public class GeneratedAnnotationResource {

    @ServerExceptionMapper
    public Response handleParseException(WebApplicationException e) {
        var cause = e.getCause() == null ? e : e.getCause();
        return Response.status(Response.Status.BAD_REQUEST).entity(cause.getMessage()).build();
    }

    // --- PropertyIgnoreBean: @JsonProperty + @JsonIgnore ---

    @GET
    @Path("/property-ignore")
    public PropertyIgnoreBean getPropertyIgnore() {
        PropertyIgnoreBean bean = new PropertyIgnoreBean();
        bean.setName("Alice");
        bean.setSecret("hidden-value");
        bean.setAge(25);
        return bean;
    }

    @POST
    @Path("/property-ignore")
    @Consumes(MediaType.APPLICATION_JSON)
    public PropertyIgnoreBean echoPropertyIgnore(PropertyIgnoreBean bean) {
        return bean;
    }

    // --- NamingWithOverrideBean: @JsonNaming + @JsonProperty + @JsonIgnore ---

    @GET
    @Path("/naming-override")
    public NamingWithOverrideBean getNamingOverride() {
        NamingWithOverrideBean bean = new NamingWithOverrideBean();
        bean.setFirstName("John");
        bean.setLastName("Doe");
        bean.setEmailAddress("john@example.com");
        bean.setInternalId("INT-001");
        return bean;
    }

    @POST
    @Path("/naming-override")
    @Consumes(MediaType.APPLICATION_JSON)
    public NamingWithOverrideBean echoNamingOverride(NamingWithOverrideBean bean) {
        return bean;
    }

    // --- CreatorAliasBean: @JsonCreator + @JsonAlias + @JsonProperty ---

    @POST
    @Path("/creator-alias")
    @Consumes(MediaType.APPLICATION_JSON)
    public CreatorAliasBean echoCreatorAlias(CreatorAliasBean bean) {
        return bean;
    }

    // --- ViewIgnoreBean: @JsonView + @JsonIgnore ---

    @GET
    @Path("/view-ignore")
    public ViewIgnoreBean getViewIgnore() {
        return createViewIgnoreBean();
    }

    @JsonView(GeneratedViews.Public.class)
    @GET
    @Path("/view-ignore-public")
    public ViewIgnoreBean getViewIgnorePublic() {
        return createViewIgnoreBean();
    }

    @JsonView(GeneratedViews.Private.class)
    @GET
    @Path("/view-ignore-private")
    public ViewIgnoreBean getViewIgnorePrivate() {
        return createViewIgnoreBean();
    }

    private static ViewIgnoreBean createViewIgnoreBean() {
        ViewIgnoreBean bean = new ViewIgnoreBean();
        bean.setPublicField("visible");
        bean.setPrivateField(42);
        bean.setIgnoredField("ignored-value");
        return bean;
    }

    // --- UnwrappedWithRenameBean: @JsonUnwrapped + @JsonProperty + @JsonIgnore ---

    @GET
    @Path("/unwrapped-rename")
    public UnwrappedWithRenameBean getUnwrappedRename() {
        UnwrappedWithRenameBean bean = new UnwrappedWithRenameBean();
        bean.setName("test");
        bean.setHidden("secret");
        UnwrappedWithRenameBean.InnerAddress address = new UnwrappedWithRenameBean.InnerAddress();
        address.setCity("NYC");
        address.setZipCode("10001");
        bean.setAddress(address);
        return bean;
    }

    // --- AnySetterIgnorePropertiesBean: @JsonAnySetter + @JsonIgnoreProperties + @JsonProperty ---

    @POST
    @Path("/any-setter-ignore-props")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String echoAnySetterIgnoreProperties(AnySetterIgnorePropertiesBean bean) {
        return "{\"id\":\"" + bean.getIdentifier()
                + "\",\"name\":\"" + bean.getName()
                + "\",\"extras_size\":" + bean.getExtras().size() + "}";
    }

    // --- PolymorphicWithPropertyBase: @JsonTypeInfo + @JsonSubTypes + @JsonTypeName + @JsonProperty ---

    @GET
    @Path("/polymorphic-text")
    public PolymorphicWithPropertyBase getPolymorphicText() {
        return new PolymorphicWithPropertyBase.TextItem("hello", "plain");
    }

    @GET
    @Path("/polymorphic-number")
    public PolymorphicWithPropertyBase getPolymorphicNumber() {
        return new PolymorphicWithPropertyBase.NumberItem(42);
    }

    // --- MultiAnnotationRecord: @JsonProperty + @JsonAlias + @JsonIgnoreProperties ---

    @POST
    @Path("/multi-annotation-record")
    @Consumes(MediaType.APPLICATION_JSON)
    public MultiAnnotationRecord echoMultiAnnotationRecord(MultiAnnotationRecord record) {
        return record;
    }

    // --- CreatorIgnoreBean: @JsonCreator + @JsonIgnore + @JsonProperty ---

    @POST
    @Path("/creator-ignore")
    @Consumes(MediaType.APPLICATION_JSON)
    public CreatorIgnoreBean echoCreatorIgnore(CreatorIgnoreBean bean) {
        return bean;
    }

    // --- ValueCreatorWrapper: @JsonValue + @JsonCreator ---

    @GET
    @Path("/value-creator")
    @Produces(MediaType.APPLICATION_JSON)
    public ValueCreatorWrapper getValueCreator() {
        return new ValueCreatorWrapper("hello world");
    }

    // --- NamingAliasRecord: @JsonNaming + @JsonAlias ---

    @GET
    @Path("/naming-alias")
    public NamingAliasRecord getNamingAlias() {
        return new NamingAliasRecord("John", "Doe");
    }

    @POST
    @Path("/naming-alias")
    @Consumes(MediaType.APPLICATION_JSON)
    public NamingAliasRecord echoNamingAlias(NamingAliasRecord record) {
        return record;
    }

    // --- PropertyViewRecord: @JsonProperty + @JsonView ---

    @GET
    @Path("/property-view")
    public PropertyViewRecord getPropertyView() {
        return new PropertyViewRecord("Alice", "SECRET", "A");
    }

    @JsonView(GeneratedViews.Public.class)
    @GET
    @Path("/property-view-public")
    public PropertyViewRecord getPropertyViewPublic() {
        return new PropertyViewRecord("Alice", "SECRET", "A");
    }

    @JsonView(GeneratedViews.Private.class)
    @GET
    @Path("/property-view-private")
    public PropertyViewRecord getPropertyViewPrivate() {
        return new PropertyViewRecord("Alice", "SECRET", "A");
    }

    // --- NamingViewBean: @JsonNaming + @JsonView + @JsonProperty ---

    @GET
    @Path("/naming-view")
    public NamingViewBean getNamingView() {
        return createNamingViewBean();
    }

    @JsonView(GeneratedViews.Public.class)
    @GET
    @Path("/naming-view-public")
    public NamingViewBean getNamingViewPublic() {
        return createNamingViewBean();
    }

    @JsonView(GeneratedViews.Private.class)
    @GET
    @Path("/naming-view-private")
    public NamingViewBean getNamingViewPrivate() {
        return createNamingViewBean();
    }

    private static NamingViewBean createNamingViewBean() {
        NamingViewBean bean = new NamingViewBean();
        bean.setFirstName("Jane");
        bean.setLastName("Smith");
        bean.setEmail("jane@example.com");
        return bean;
    }

    // --- IgnorePropertiesCreatorRecord: @JsonIgnoreProperties + @JsonProperty ---

    @POST
    @Path("/ignore-props-creator")
    @Consumes(MediaType.APPLICATION_JSON)
    public IgnorePropertiesCreatorRecord echoIgnorePropsCreator(IgnorePropertiesCreatorRecord record) {
        return record;
    }

    // --- IgnoreAnySetterBean: @JsonIgnore + @JsonAnySetter + @JsonProperty ---

    @POST
    @Path("/ignore-any-setter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String echoIgnoreAnySetter(IgnoreAnySetterBean bean) {
        return "{\"name\":\"" + bean.getName()
                + "\",\"others_size\":" + bean.getOthers().size() + "}";
    }

    // --- Lists/collections of annotated types ---

    @POST
    @Path("/property-ignore-list")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<PropertyIgnoreBean> echoPropertyIgnoreList(List<PropertyIgnoreBean> list) {
        return list;
    }

    @GET
    @Path("/polymorphic-list")
    public List<PolymorphicWithPropertyBase> getPolymorphicList() {
        return List.of(
                new PolymorphicWithPropertyBase.TextItem("first", "html"),
                new PolymorphicWithPropertyBase.NumberItem(99));
    }

    // --- AnyGetterBean: @JsonAnyGetter + @JsonAnySetter ---

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

    // --- ManagedReferenceParent/Child: @JsonManagedReference + @JsonBackReference ---

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

    // --- DateFormatBean: @JsonFormat with date pattern ---

    @GET
    @Path("/date-format")
    public DateFormatBean getDateFormat() {
        DateFormatBean bean = new DateFormatBean();
        bean.setName("date-test");
        bean.setDate(Date.from(LocalDate.of(2025, 6, 15).atStartOfDay().toInstant(ZoneOffset.UTC)));
        return bean;
    }

    // --- ZonedDateTimeFormatBean: @JsonFormat with ZonedDateTime ---

    @GET
    @Path("/zoned-date-format")
    public ZonedDateTimeFormatBean getZonedDateFormat() {
        ZonedDateTimeFormatBean bean = new ZonedDateTimeFormatBean();
        bean.setName("zoned-date-test");
        bean.setDateTime(ZonedDateTime.of(2024, 3, 13, 10, 5, 1, 0, ZoneOffset.UTC));
        return bean;
    }

    // --- NumberShapedTemporalBean: @JsonFormat(shape=NUMBER) with Instant and Duration ---

    @GET
    @Path("/number-shaped-temporal")
    public NumberShapedTemporalBean getNumberShapedTemporal() {
        NumberShapedTemporalBean bean = new NumberShapedTemporalBean();
        bean.setName("number-shaped");
        bean.setInstant(Instant.ofEpochSecond(1710324301L, 500_000_000));
        bean.setDuration(Duration.ofSeconds(76975, 500_000_000));
        return bean;
    }

    // --- DurationFormatBean: @JsonFormat(shape=STRING) with Duration ---

    @GET
    @Path("/duration-format")
    public DurationFormatBean getDurationFormat() {
        DurationFormatBean bean = new DurationFormatBean();
        bean.setName("duration-test");
        bean.setDuration(Duration.ofHours(21).plusMinutes(22).plusSeconds(55));
        return bean;
    }

    // --- FormatBean: @JsonFormat ---

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

    // --- FormatStringShapeBean: @JsonFormat(shape = STRING) on primitives ---

    @GET
    @Path("/format-string-shape")
    public FormatStringShapeBean getFormatStringShape() {
        FormatStringShapeBean bean = new FormatStringShapeBean();
        bean.setName("string-shape");
        bean.setCount(42);
        bean.setScore(3.14);
        bean.setActive(true);
        bean.setBigNumber(123456789L);
        return bean;
    }

    @POST
    @Path("/format-string-shape")
    @Consumes(MediaType.APPLICATION_JSON)
    public FormatStringShapeBean echoFormatStringShape(FormatStringShapeBean bean) {
        return bean;
    }

    // --- FormatNumberBooleanBean: @JsonFormat(shape = NUMBER) on boolean ---

    @GET
    @Path("/format-number-boolean")
    public FormatNumberBooleanBean getFormatNumberBoolean() {
        FormatNumberBooleanBean bean = new FormatNumberBooleanBean();
        bean.setName("bool-as-number");
        bean.setEnabled(true);
        bean.setOptional(false);
        return bean;
    }

    @POST
    @Path("/format-number-boolean")
    @Consumes(MediaType.APPLICATION_JSON)
    public FormatNumberBooleanBean echoFormatNumberBoolean(FormatNumberBooleanBean bean) {
        return bean;
    }

    // --- FormatDateTimestampBean: @JsonFormat(shape = NUMBER) on Date ---

    @GET
    @Path("/format-date-timestamp")
    public FormatDateTimestampBean getFormatDateTimestamp() {
        FormatDateTimestampBean bean = new FormatDateTimestampBean();
        bean.setName("timestamp-test");
        bean.setTimestamp(new Date(1750000000000L));
        return bean;
    }

    @POST
    @Path("/format-date-timestamp")
    @Consumes(MediaType.APPLICATION_JSON)
    public FormatDateTimestampBean echoFormatDateTimestamp(FormatDateTimestampBean bean) {
        return bean;
    }

    // --- GetterSetterBean: @JsonGetter + @JsonSetter ---

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

    // --- IgnoreTypeBean: @JsonIgnoreType ---

    @GET
    @Path("/ignore-type")
    public IgnoreTypeBean getIgnoreType() {
        IgnoreTypeBean bean = new IgnoreTypeBean();
        bean.setName("visible");
        bean.setMetadata(new IgnoredType("secret-data"));
        return bean;
    }

    // --- IncludeBean: @JsonInclude ---

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

    // --- PropertyOrderBean: @JsonPropertyOrder ---

    @GET
    @Path("/property-order")
    public PropertyOrderBean getPropertyOrder() {
        PropertyOrderBean bean = new PropertyOrderBean();
        bean.setAlpha("a");
        bean.setMiddle("m");
        bean.setZebra("z");
        return bean;
    }

    // --- RawValueBean: @JsonRawValue ---

    @GET
    @Path("/raw-value")
    public RawValueBean getRawValue() {
        RawValueBean bean = new RawValueBean();
        bean.setName("test");
        bean.setRawJson("{\"nested\":\"value\",\"count\":1}");
        return bean;
    }

    // --- PackageProtectedBean: package-protected class with single int constructor ---

    @GET
    @Path("/package-protected")
    public PackageProtectedBean getPackageProtected() {
        PackageProtectedBean bean = new PackageProtectedBean(42);
        bean.setLabel("custom-label");
        return bean;
    }

    @POST
    @Path("/package-protected")
    @Consumes(MediaType.APPLICATION_JSON)
    public PackageProtectedBean echoPackageProtected(PackageProtectedBean bean) {
        return bean;
    }

    // --- UnwrappedIgnorePropertiesBean: @JsonUnwrapped + @JsonIgnoreProperties ---

    @GET
    @Path("/unwrapped-ignore-props")
    public UnwrappedIgnorePropertiesBean getUnwrappedIgnoreProps() {
        UnwrappedIgnorePropertiesBean bean = new UnwrappedIgnorePropertiesBean();
        bean.setId("u1");
        UnwrappedIgnorePropertiesBean.User user = new UnwrappedIgnorePropertiesBean.User();
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("secret123");
        bean.setUser(user);
        return bean;
    }

    // --- TestWithJsonPropertyDto: @JsonProperty renames field ---

    @GET
    @Path("/json-property-rename")
    public TestWithJsonPropertyDto getJsonPropertyRename() {
        return new TestWithJsonPropertyDto("Alice");
    }

    @POST
    @Path("/json-property-rename")
    @Consumes(MediaType.APPLICATION_JSON)
    public TestWithJsonPropertyDto echoJsonPropertyRename(TestWithJsonPropertyDto dto) {
        return dto;
    }

    // --- SpecialCharPropertyBean: @JsonProperty with special characters (hyphens, dots) ---

    @GET
    @Path("/special-char-property")
    public SpecialCharPropertyBean getSpecialCharProperty() {
        SpecialCharPropertyBean bean = new SpecialCharPropertyBean();
        bean.setRoundValue(1);
        bean.setNormalName("test");
        return bean;
    }

    @POST
    @Path("/special-char-property")
    @Consumes(MediaType.APPLICATION_JSON)
    public SpecialCharPropertyBean echoSpecialCharProperty(SpecialCharPropertyBean bean) {
        return bean;
    }

    // --- FormatArrayShapeBean: @JsonFormat(shape = ARRAY) on class ---

    @GET
    @Path("/format-array-shape")
    public FormatArrayShapeBean getFormatArrayShape() {
        return new FormatArrayShapeBean(1.0, 2.0);
    }

    @POST
    @Path("/format-array-shape")
    @Consumes(MediaType.APPLICATION_JSON)
    public FormatArrayShapeBean echoFormatArrayShape(FormatArrayShapeBean bean) {
        return bean;
    }

    @POST
    @Path("/format-array-shape-list")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<FormatArrayShapeBean> echoFormatArrayShapeList(List<FormatArrayShapeBean> list) {
        return list;
    }

    // --- FormatArrayShapeNoOrderBean: @JsonFormat(shape = ARRAY) without @JsonPropertyOrder ---

    @GET
    @Path("/format-array-shape-no-order")
    public FormatArrayShapeNoOrderBean getFormatArrayShapeNoOrder() {
        return new FormatArrayShapeNoOrderBean("hello", 42);
    }

    @POST
    @Path("/format-array-shape-no-order")
    @Consumes(MediaType.APPLICATION_JSON)
    public FormatArrayShapeNoOrderBean echoFormatArrayShapeNoOrder(FormatArrayShapeNoOrderBean bean) {
        return bean;
    }

    // --- UnwrappedWithPrefixBean: @JsonUnwrapped with prefix ---

    @GET
    @Path("/unwrapped-prefix")
    public UnwrappedWithPrefixBean getUnwrappedPrefix() {
        UnwrappedWithPrefixBean bean = new UnwrappedWithPrefixBean();
        bean.setOrderId("ORD-001");
        UnwrappedWithPrefixBean.Address address = new UnwrappedWithPrefixBean.Address();
        address.setCity("Rome");
        address.setZipCode("00100");
        bean.setBillingAddress(address);
        return bean;
    }

    @POST
    @Path("/unwrapped-prefix")
    @Consumes(MediaType.APPLICATION_JSON)
    public UnwrappedWithPrefixBean echoUnwrappedPrefix(UnwrappedWithPrefixBean bean) {
        return bean;
    }
}
