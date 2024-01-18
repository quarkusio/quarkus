package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HyphenateEnumConverterTest {
    enum MyEnum {
        DISCARD,
        READ_UNCOMMITTED,
        SIGUSR1,
        TrendBreaker,
        MAKING_LifeDifficult,
        YeOldeJBoss,
    }

    enum MyOtherEnum {
        makingLifeDifficult,
        READ__UNCOMMITTED
    }

    @Test
    void convertMyEnum() {
        HyphenateEnumConverter<MyEnum> converter = new HyphenateEnumConverter<>(MyEnum.class);
        MyEnum myEnum = converter.convert("DISCARD");
        assertEquals(myEnum, MyEnum.DISCARD);
        myEnum = converter.convert("discard");
        assertEquals(myEnum, MyEnum.DISCARD);
        myEnum = converter.convert("READ_UNCOMMITTED");
        assertEquals(myEnum, MyEnum.READ_UNCOMMITTED);
        myEnum = converter.convert("read-uncommitted");
        assertEquals(myEnum, MyEnum.READ_UNCOMMITTED);
        myEnum = converter.convert("SIGUSR1");
        assertEquals(myEnum, MyEnum.SIGUSR1);
        myEnum = converter.convert("sigusr1");
        assertEquals(myEnum, MyEnum.SIGUSR1);
        myEnum = converter.convert("TrendBreaker");
        assertEquals(myEnum, MyEnum.TrendBreaker);
        myEnum = converter.convert("trend-breaker");
        assertEquals(myEnum, MyEnum.TrendBreaker);
        myEnum = converter.convert("MAKING_LifeDifficult");
        assertEquals(myEnum, MyEnum.MAKING_LifeDifficult);
        myEnum = converter.convert("making-life-difficult");
        assertEquals(myEnum, MyEnum.MAKING_LifeDifficult);
        myEnum = converter.convert("YeOldeJBoss");
        assertEquals(myEnum, MyEnum.YeOldeJBoss);
        myEnum = converter.convert("ye-olde-jboss");
        assertEquals(myEnum, MyEnum.YeOldeJBoss);
    }

    @Test
    void convertMyOtherEnum() {
        HyphenateEnumConverter<MyOtherEnum> converter = new HyphenateEnumConverter<>(MyOtherEnum.class);
        MyOtherEnum myOtherEnum = converter.convert("makingLifeDifficult");
        assertEquals(myOtherEnum, MyOtherEnum.makingLifeDifficult);
        myOtherEnum = converter.convert("making-life-difficult");
        assertEquals(myOtherEnum, MyOtherEnum.makingLifeDifficult);
        myOtherEnum = converter.convert("READ__UNCOMMITTED");
        assertEquals(myOtherEnum, MyOtherEnum.READ__UNCOMMITTED);
        myOtherEnum = converter.convert("read-uncommitted");
        assertEquals(myOtherEnum, MyOtherEnum.READ__UNCOMMITTED);
    }

    @Test
    void illegalEnumConfigUtilConversion() {
        HyphenateEnumConverter<MyEnum> hyphenateEnumConverter = new HyphenateEnumConverter<>(MyEnum.class);
        assertThrows(IllegalArgumentException.class, () -> hyphenateEnumConverter.convert("READUNCOMMITTED"));
    }

    enum HTTPConduit {
        QuarkusCXFDefault,
        CXFDefault,
        HttpClientHTTPConduitFactory,
        URLConnectionHTTPConduitFactory
    }

    @Test
    void convertHttpConduit() {
        HyphenateEnumConverter<HTTPConduit> converter = new HyphenateEnumConverter<>(HTTPConduit.class);
        assertEquals(HTTPConduit.QuarkusCXFDefault, converter.convert("QuarkusCXFDefault"));
        assertEquals(HTTPConduit.QuarkusCXFDefault, converter.convert("quarkus-cxf-default"));
        assertEquals(HTTPConduit.CXFDefault, converter.convert("CXFDefault"));
        assertEquals(HTTPConduit.CXFDefault, converter.convert("cxf-default"));
        assertEquals(HTTPConduit.HttpClientHTTPConduitFactory, converter.convert("HttpClientHTTPConduitFactory"));
        assertEquals(HTTPConduit.HttpClientHTTPConduitFactory, converter.convert("http-client-http-conduit-factory"));
        assertEquals(HTTPConduit.URLConnectionHTTPConduitFactory, converter.convert("URLConnectionHTTPConduitFactory"));
        assertEquals(HTTPConduit.URLConnectionHTTPConduitFactory, converter.convert("url-connection-http-conduit-factory"));
    }
}
