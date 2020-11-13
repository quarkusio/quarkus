package io.quarkus.panache.common.deployment;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.impl.GenerateBridge;

public abstract class PanacheEntityEnhancer<MetamodelType extends MetamodelInfo<?>>
        implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public static final DotName DOTNAME_GENERATE_BRIDGE = DotName.createSimple(GenerateBridge.class.getName());

    public static final String JAXB_ANNOTATION_PREFIX = "Ljavax/xml/bind/annotation/";
    private static final String JAXB_TRANSIENT_BINARY_NAME = "javax/xml/bind/annotation/XmlTransient";
    public static final String JAXB_TRANSIENT_SIGNATURE = "L" + JAXB_TRANSIENT_BINARY_NAME + ";";

    private static final String JSON_PROPERTY_BINARY_NAME = "com/fasterxml/jackson/annotation/JsonProperty";
    public static final String JSON_PROPERTY_SIGNATURE = "L" + JSON_PROPERTY_BINARY_NAME + ";";

    public static final DotName JSON_IGNORE_DOT_NAME = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnore");
    public static final DotName JSON_PROPERTY_DOT_NAME = DotName.createSimple("com.fasterxml.jackson.annotation.JsonProperty");

    protected MetamodelType modelInfo;
    protected final IndexView indexView;
    protected final List<PanacheMethodCustomizer> methodCustomizers;

    public PanacheEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        this.indexView = index;
        this.methodCustomizers = methodCustomizers;
    }

    @Override
    public abstract ClassVisitor apply(String className, ClassVisitor outputClassVisitor);

    public abstract void collectFields(ClassInfo classInfo);

    public MetamodelType getModelInfo() {
        return modelInfo;
    }
}
