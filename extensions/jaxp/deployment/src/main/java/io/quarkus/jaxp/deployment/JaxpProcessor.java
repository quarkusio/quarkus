package io.quarkus.jaxp.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class JaxpProcessor {

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl",
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
                "com.sun.org.apache.xerces.internal.parsers.SAXParser",
                "com.sun.org.apache.xml.internal.utils.FastStringBuffer"));

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "com.sun.xml.internal.stream.XMLInputFactoryImpl",
                "com.sun.xml.internal.stream.XMLOutputFactoryImpl",
                "com.sun.org.apache.xpath.internal.functions.FuncNot",
                "com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl",
                "javax.xml.namespace.QName"));
    }

    @BuildStep
    void resourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        Stream.of(
                "com.sun.org.apache.xml.internal.serializer.utils.SerializerMessages",
                "com.sun.org.apache.xml.internal.res.XMLErrorResources",
                "com.sun.org.apache.xerces.internal.impl.msg.SAXMessages",
                "com.sun.org.apache.xerces.internal.impl.msg.XMLMessages",
                "com.sun.org.apache.xerces.internal.impl.msg.XMLSchemaMessages",
                "com.sun.org.apache.xerces.internal.impl.xpath.regex.message")
                .map(NativeImageResourceBundleBuildItem::new)
                .forEach(resourceBundle::produce);
    }

    @BuildStep
    void resources(BuildProducer<NativeImageResourceBuildItem> resource) {

        Stream.of(
                "html",
                "text",
                "xml",
                "unknown")
                .map(s -> "com/sun/org/apache/xml/internal/serializer/output_" + s + ".properties")
                .map(NativeImageResourceBuildItem::new)
                .forEach(resource::produce);

    }

}
