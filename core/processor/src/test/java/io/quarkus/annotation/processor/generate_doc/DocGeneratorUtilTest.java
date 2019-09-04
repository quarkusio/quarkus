package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.AGROAL_API_JAVA_DOC_SITE;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.OFFICIAL_JAVA_DOC_BASE_LINK;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.VERTX_JAVA_DOC_SITE;
import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.getJavaDocSiteLink;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;

import org.junit.Test;

import io.quarkus.annotation.processor.Constants;

public class DocGeneratorUtilTest {
    @Test
    public void shouldReturnEmptyListForPrimitiveValue() {
        String value = getJavaDocSiteLink("int");
        assertEquals(Constants.EMPTY, value);

        value = getJavaDocSiteLink("long");
        assertEquals(Constants.EMPTY, value);

        value = getJavaDocSiteLink("float");
        assertEquals(Constants.EMPTY, value);

        value = getJavaDocSiteLink("boolean");
        assertEquals(Constants.EMPTY, value);

        value = getJavaDocSiteLink("double");
        assertEquals(Constants.EMPTY, value);

        value = getJavaDocSiteLink("char");
        assertEquals(Constants.EMPTY, value);

        value = getJavaDocSiteLink("short");
        assertEquals(Constants.EMPTY, value);

        value = getJavaDocSiteLink("byte");
        assertEquals(Constants.EMPTY, value);
    }

    @Test
    public void shouldReturnALinkToOfficialJavaDocIfIsJavaOfficialType() {
        String value = getJavaDocSiteLink(String.class.getName());
        assertEquals(OFFICIAL_JAVA_DOC_BASE_LINK + "java/lang/String.html", value);

        value = getJavaDocSiteLink(Integer.class.getName());
        assertEquals(OFFICIAL_JAVA_DOC_BASE_LINK + "java/lang/Integer.html", value);

        value = getJavaDocSiteLink(InetAddress.class.getName());
        assertEquals(OFFICIAL_JAVA_DOC_BASE_LINK + "java/net/InetAddress.html", value);

        value = getJavaDocSiteLink(BigInteger.class.getName());
        assertEquals(OFFICIAL_JAVA_DOC_BASE_LINK + "java/math/BigInteger.html", value);

        value = getJavaDocSiteLink(Duration.class.getName());
        assertEquals(OFFICIAL_JAVA_DOC_BASE_LINK + "java/time/Duration.html", value);

        value = getJavaDocSiteLink((Map.Entry.class.getName().replace('$', '.')));
        assertEquals(OFFICIAL_JAVA_DOC_BASE_LINK + "java/util/Map.Entry.html", value);

        value = getJavaDocSiteLink(Map.Entry.class.getName());
        assertEquals(OFFICIAL_JAVA_DOC_BASE_LINK + "java/util/Map.Entry.html", value);
    }

    @Test
    public void shouldReturnALinkToAgroalJavaDocIfTypeIsDeclaredInAgroalPackage() {
        String value = getJavaDocSiteLink(
                "io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation");
        assertEquals(AGROAL_API_JAVA_DOC_SITE
                + "io/agroal/api/configuration/AgroalConnectionFactoryConfiguration.TransactionIsolation.html", value);

        value = getJavaDocSiteLink("io.agroal.api.AgroalDataSource.FlushMode");
        assertEquals(AGROAL_API_JAVA_DOC_SITE + "io/agroal/api/AgroalDataSource.FlushMode.html", value);
    }

    @Test
    public void shouldReturnALinkToVertxJavaDocIfTypeIsDeclaredInVertxPackage() {
        String value = getJavaDocSiteLink(
                "io.vertx.core.Context");
        assertEquals(VERTX_JAVA_DOC_SITE + "io/vertx/core/Context.html", value);

        value = getJavaDocSiteLink("io.vertx.amqp.AmqpMessage");
        assertEquals(VERTX_JAVA_DOC_SITE + "io/vertx/amqp/AmqpMessage.html", value);
    }

    @Test
    public void shouldReturnEmptyLinkIfUnknownJavaDocType() {
        String value = getJavaDocSiteLink("io.quarkus.ConfigItem");
        assertEquals(Constants.EMPTY, value);
    }
}
