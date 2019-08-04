package io.quarkus.runtime.configuration;

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(myEnum, MyEnum.DISCARD);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("discard");
        Assert.assertEquals(myEnum, MyEnum.DISCARD);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("READ_UNCOMMITTED");
        Assert.assertEquals(myEnum, MyEnum.READ_UNCOMMITTED);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("read-uncommitted");
        Assert.assertEquals(myEnum, MyEnum.READ_UNCOMMITTED);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("SIGUSR1");
        Assert.assertEquals(myEnum, MyEnum.SIGUSR1);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("sigusr1");
        Assert.assertEquals(myEnum, MyEnum.SIGUSR1);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("TrendBreaker");
        Assert.assertEquals(myEnum, MyEnum.TrendBreaker);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("trend-breaker");
        Assert.assertEquals(myEnum, MyEnum.TrendBreaker);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("MAKING_LifeDifficult");
        Assert.assertEquals(myEnum, MyEnum.MAKING_LifeDifficult);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("making-life-difficult");
        Assert.assertEquals(myEnum, MyEnum.MAKING_LifeDifficult);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("YeOldeJBoss");
        Assert.assertEquals(myEnum, MyEnum.YeOldeJBoss);
        myEnum = (MyEnum) hyphenateEnumConverter.convert("ye-olde-jboss");
        Assert.assertEquals(myEnum, MyEnum.YeOldeJBoss);
    }

    @Test
    public void convertMyOtherEnum() {
        hyphenateEnumConverter = new HyphenateEnumConverter(MyOtherEnum.class);
        MyOtherEnum myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("makingLifeDifficult");
        Assert.assertEquals(myOtherEnum, MyOtherEnum.makingLifeDifficult);
        myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("making-life-difficult");
        Assert.assertEquals(myOtherEnum, MyOtherEnum.makingLifeDifficult);
        myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("READ__UNCOMMITTED");
        Assert.assertEquals(myOtherEnum, MyOtherEnum.READ__UNCOMMITTED);
        myOtherEnum = (MyOtherEnum) hyphenateEnumConverter.convert("read-uncommitted");
        Assert.assertEquals(myOtherEnum, MyOtherEnum.READ__UNCOMMITTED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalEnumConfigUtilConversion() {
        hyphenateEnumConverter = new HyphenateEnumConverter(MyEnum.class);
        System.out.println(hyphenateEnumConverter.convert("READUNCOMMITTED"));
    }
}
