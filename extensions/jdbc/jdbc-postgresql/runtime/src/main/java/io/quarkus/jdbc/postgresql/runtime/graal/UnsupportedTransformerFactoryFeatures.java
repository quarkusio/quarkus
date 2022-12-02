package io.quarkus.jdbc.postgresql.runtime.graal;

import javax.xml.XMLConstants;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Some features which are being set by DefaultPGXmlFactoryFactory#setFactoryProperties
 * by default are not supported by the current GraalVM version: when using them they
 * will result in fatal errors.
 * These same features are also not supported in JVM mode, except that when running in JVM
 * these errors are caught and ignored, rather than crashing the application; so the solution
 * seems simple: ignore these features.
 */
@TargetClass(className = "org.postgresql.xml.DefaultPGXmlFactoryFactory")
public final class UnsupportedTransformerFactoryFeatures {

    @Substitute
    private static void setFactoryProperties(Object factory) {
        setFeatureQuietly(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        //		setFeatureQuietly(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        //		setFeatureQuietly(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        //		setFeatureQuietly(factory, "http://xml.org/sax/features/external-general-entities", false);
        //		setFeatureQuietly(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        // Values from XMLConstants inlined for JDK 1.6 compatibility
        setAttributeQuietly(factory, "http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        //		setAttributeQuietly(factory, "http://javax.xml.XMLConstants/property/accessExternalSchema", "");
        setAttributeQuietly(factory, "http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    }

    @Alias
    private static void setFeatureQuietly(Object factory, String name, boolean value) {
        //no-op : will use the original code
    }

    @Alias
    private static void setAttributeQuietly(Object factory, String name, Object value) {
        //no-op : will use the original code
    }

}
