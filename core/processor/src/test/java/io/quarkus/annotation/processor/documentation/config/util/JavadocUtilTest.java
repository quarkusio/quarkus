package io.quarkus.annotation.processor.documentation.config.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigInteger;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadoc;
import io.quarkus.annotation.processor.documentation.config.discovery.ParsedJavadocSection;

public class JavadocUtilTest {

    @Test
    public void parseNullJavaDoc() {
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(null);
        assertNull(parsed.description());
    }

    @Test
    public void parseSimpleJavaDoc() {
        String javaDoc = "hello world";
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc(javaDoc);

        assertEquals(javaDoc, parsed.description());
    }

    @Test
    public void since() {
        ParsedJavadoc parsed = JavadocUtil.parseConfigItemJavadoc("Javadoc text\n\n@since 1.2.3");
        assertEquals("Javadoc text", parsed.description());
        assertEquals("1.2.3", parsed.since());
    }

    @Test
    public void deprecated() {
        ParsedJavadoc parsed = JavadocUtil
                .parseConfigItemJavadoc("@deprecated JNI is always enabled starting from GraalVM 19.3.1.");
        assertEquals(null, parsed.description());
    }

    @Test
    public void parseNullSection() {
        ParsedJavadocSection parsed = JavadocUtil.parseConfigSectionJavadoc(null);
        assertEquals(null, parsed.details());
        assertEquals(null, parsed.title());
    }

    @Test
    public void parseSimpleSection() {
        ParsedJavadocSection parsed = JavadocUtil.parseConfigSectionJavadoc("title");
        assertEquals("title", parsed.title());
        assertEquals(null, parsed.details());
    }

    @Test
    public void parseSectionWithIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section .Introduction";
        String expectedDetails = "Introduction";
        String expectedTitle = "Config Section";
        assertEquals(expectedTitle, JavadocUtil.parseConfigSectionJavadoc(javaDoc).title());
        assertEquals(expectedDetails, JavadocUtil.parseConfigSectionJavadoc(javaDoc).details());

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section </p>. Introduction";
        expectedDetails = "Introduction";
        assertEquals(expectedDetails, JavadocUtil.parseConfigSectionJavadoc(javaDoc).details());
        assertEquals(expectedTitle, JavadocUtil.parseConfigSectionJavadoc(javaDoc).title());
    }

    @Test
    public void parseSectionWithParagraph() {
        String javaDoc = "Dev Services\n<p>\nDev Services allows Quarkus to automatically start Elasticsearch in dev and test mode.";
        assertEquals("Dev Services", JavadocUtil.parseConfigSectionJavadoc(javaDoc).title());
    }

    @Test
    public void properlyParseConfigSectionWrittenInHtml() {
        String javaDoc = "Config Section.<p>This is section introduction";
        String expectedDetails = "<p>This is section introduction";
        String title = "Config Section";
        assertEquals(expectedDetails, JavadocUtil.parseConfigSectionJavadoc(javaDoc).details());
        assertEquals(title, JavadocUtil.parseConfigSectionJavadoc(javaDoc).title());
    }

    @Test
    public void parseSectionWithoutIntroduction() {
        /**
         * Simple javadoc
         */
        String javaDoc = "Config Section";
        String expectedTitle = "Config Section";
        String expectedDetails = null;
        ParsedJavadocSection sectionHolder = JavadocUtil.parseConfigSectionJavadoc(javaDoc);
        assertEquals(expectedDetails, sectionHolder.details());
        assertEquals(expectedTitle, sectionHolder.title());

        javaDoc = "Config Section.";
        expectedTitle = "Config Section";
        expectedDetails = null;
        sectionHolder = JavadocUtil.parseConfigSectionJavadoc(javaDoc);
        assertEquals(expectedDetails, sectionHolder.details());
        assertEquals(expectedTitle, sectionHolder.title());

        /**
         * html javadoc
         */
        javaDoc = "<p>Config Section</p>";
        expectedTitle = "Config Section";
        expectedDetails = null;
        sectionHolder = JavadocUtil.parseConfigSectionJavadoc(javaDoc);
        assertEquals(expectedDetails, sectionHolder.details());
        assertEquals(expectedTitle, sectionHolder.title());
    }

    @Test
    public void shouldReturnEmptyListForPrimitiveValue() {
        String value = JavadocUtil.getJavadocSiteLink("int");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink("long");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink("float");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink("boolean");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink("double");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink("char");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink("short");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink("byte");
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Boolean.class.getName());
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Byte.class.getName());
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Short.class.getName());
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Integer.class.getName());
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Long.class.getName());
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Float.class.getName());
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Double.class.getName());
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(Character.class.getName());
        assertNull(value);
    }

    @Test
    public void shouldReturnALinkToOfficialJavaDocIfIsJavaOfficialType() {
        String value = JavadocUtil.getJavadocSiteLink(String.class.getName());
        // for String, we don't return a Javadoc link as it's a very basic type
        assertNull(value);

        value = JavadocUtil.getJavadocSiteLink(InetAddress.class.getName());
        assertEquals(JavadocUtil.OFFICIAL_JAVA_DOC_BASE_LINK + "java/net/InetAddress.html", value);

        value = JavadocUtil.getJavadocSiteLink(BigInteger.class.getName());
        assertEquals(JavadocUtil.OFFICIAL_JAVA_DOC_BASE_LINK + "java/math/BigInteger.html", value);

        value = JavadocUtil.getJavadocSiteLink(Duration.class.getName());
        assertEquals(JavadocUtil.OFFICIAL_JAVA_DOC_BASE_LINK + "java/time/Duration.html", value);

        value = JavadocUtil.getJavadocSiteLink((Map.Entry.class.getName().replace('$', '.')));
        assertEquals(JavadocUtil.OFFICIAL_JAVA_DOC_BASE_LINK + "java/util/Map.Entry.html", value);

        value = JavadocUtil.getJavadocSiteLink(Map.Entry.class.getName());
        assertEquals(JavadocUtil.OFFICIAL_JAVA_DOC_BASE_LINK + "java/util/Map.Entry.html", value);

        value = JavadocUtil.getJavadocSiteLink(List.class.getName());
        assertEquals(JavadocUtil.OFFICIAL_JAVA_DOC_BASE_LINK + "java/util/List.html", value);

        value = JavadocUtil.getJavadocSiteLink("java.util.List<java.lang.String>");
        assertEquals(JavadocUtil.OFFICIAL_JAVA_DOC_BASE_LINK + "java/util/List.html", value);
    }

    @Test
    public void shouldReturnALinkToAgroalJavaDocIfTypeIsDeclaredInAgroalPackage() {
        String value = JavadocUtil.getJavadocSiteLink(
                "io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation");
        assertEquals(JavadocUtil.AGROAL_API_JAVA_DOC_SITE
                + "io/agroal/api/configuration/AgroalConnectionFactoryConfiguration.TransactionIsolation.html", value);

        value = JavadocUtil.getJavadocSiteLink("io.agroal.api.AgroalDataSource.FlushMode");
        assertEquals(JavadocUtil.AGROAL_API_JAVA_DOC_SITE + "io/agroal/api/AgroalDataSource.FlushMode.html", value);
    }

    @Test
    public void shouldReturnALinkToVertxJavaDocIfTypeIsDeclaredInVertxPackage() {
        String value = JavadocUtil.getJavadocSiteLink(
                "io.vertx.core.Context");
        assertEquals(JavadocUtil.VERTX_JAVA_DOC_SITE + "io/vertx/core/Context.html", value);

        value = JavadocUtil.getJavadocSiteLink("io.vertx.amqp.AmqpMessage");
        assertEquals(JavadocUtil.VERTX_JAVA_DOC_SITE + "io/vertx/amqp/AmqpMessage.html", value);
    }

    @Test
    public void shouldReturnEmptyLinkIfUnknownJavaDocType() {
        String value = JavadocUtil.getJavadocSiteLink("io.quarkus.ConfigDocKey");
        assertNull(value);
    }
}
