package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class HyphenateEnumConverterTest {
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

    HyphenateEnumConverter hyphenateEnumConverter;

    @Test
    public void convertMyEnum() {
        hyphenateEnumConverter = new HyphenateEnumConverter(MyEnum.class);
        MyEnum myEnum = (MyEnum) hyphenateEnumConverter.convert("DISCARD");
        assertEquals(myEnum, MyEnum.DISCARD);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("discard");
        assertEquals(myEnum, MyEnum.DISCARD);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("READ_UNCOMMITTED");
        assertEquals(myEnum, MyEnum.READ_UNCOMMITTED);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("read-uncommitted");
        assertEquals(myEnum, MyEnum.READ_UNCOMMITTED);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("SIGUSR1");
        assertEquals(myEnum, MyEnum.SIGUSR1);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("sigusr1");
        assertEquals(myEnum, MyEnum.SIGUSR1);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("TrendBreaker");
        assertEquals(myEnum, MyEnum.TrendBreaker);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("trend-breaker");
        assertEquals(myEnum, MyEnum.TrendBreaker);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("MAKING_LifeDifficult");
        assertEquals(myEnum, MyEnum.MAKING_LifeDifficult);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("making-life-difficult");
        assertEquals(myEnum, MyEnum.MAKING_LifeDifficult);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("YeOldeJBoss");
        assertEquals(myEnum, MyEnum.YeOldeJBoss);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("ye-olde-jboss");
        assertEquals(myEnum, MyEnum.YeOldeJBoss);
    }

    @Test
    public void convertMyOtherEnum() {
        hyphenateEnumConverter = new HyphenateEnumConverter(MyOtherEnum.class);
        MyOtherEnum myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("makingLifeDifficult");
        assertEquals(myOtherEnum, MyOtherEnum.makingLifeDifficult);
        myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("making-life-difficult");
        assertEquals(myOtherEnum, MyOtherEnum.makingLifeDifficult);
        myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("READ__UNCOMMITTED");
        assertEquals(myOtherEnum, MyOtherEnum.READ__UNCOMMITTED);
        myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("read-uncommitted");
        assertEquals(myOtherEnum, MyOtherEnum.READ__UNCOMMITTED);
    }

    @Test
    public void testIllegalEnumConfigUtilConversion() {
        hyphenateEnumConverter = new HyphenateEnumConverter(MyEnum.class);
        assertThrows(IllegalArgumentException.class, () -> hyphenateEnumConverter.convert("READUNCOMMITTED"));
    }
}
