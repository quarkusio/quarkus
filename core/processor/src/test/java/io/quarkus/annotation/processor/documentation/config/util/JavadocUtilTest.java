package io.quarkus.annotation.processor.documentation.config.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigInteger;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class JavadocUtilTest {

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
