package io.quarkus.panache.common.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.panache.common.impl.GenerateBridge;

public final class PanacheConstants {

    private PanacheConstants() {
    }

    public static final DotName DOTNAME_GENERATE_BRIDGE = DotName.createSimple(GenerateBridge.class.getName());

    public static final String JAXB_ANNOTATION_PREFIX = "Ljakarta/xml/bind/annotation/";
    private static final String JAXB_TRANSIENT_BINARY_NAME = "jakarta/xml/bind/annotation/XmlTransient";
    public static final String JAXB_TRANSIENT_SIGNATURE = "L" + JAXB_TRANSIENT_BINARY_NAME + ";";

    private static final String JSON_PROPERTY_BINARY_NAME = "com/fasterxml/jackson/annotation/JsonProperty";
    public static final String JSON_PROPERTY_SIGNATURE = "L" + JSON_PROPERTY_BINARY_NAME + ";";
    public static final String JSON_PROPERTY_ACCESS_SIGNATURE = "L" + JSON_PROPERTY_BINARY_NAME + "$Access;";

    public static final DotName JSON_IGNORE_DOT_NAME = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnore");
    public static final DotName JSON_PROPERTY_DOT_NAME = DotName.createSimple("com.fasterxml.jackson.annotation.JsonProperty");

    public static final String JSON_PROPERTY_VALUE = "value";
    public static final String JSON_PROPERTY_NAMESPACE = "namespace";
    public static final String JSON_PROPERTY_REQUIRED = "required";
    public static final String JSON_PROPERTY_INDEX = "index";
    public static final String JSON_PROPERTY_DEFAULT_VALUE = "defaultValue";
    public static final String JSON_PROPERTY_ACCESS = "access";

}
