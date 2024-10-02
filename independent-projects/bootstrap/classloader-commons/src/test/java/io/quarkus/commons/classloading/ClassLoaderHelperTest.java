package io.quarkus.commons.classloading;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassLoaderHelperTest {

    @Test
    public void testFromClassToResourceName() {
        Assertions.assertEquals("java/lang/String.class", ClassLoaderHelper.fromClassNameToResourceName("java.lang.String"));
        Assertions.assertEquals(".class", ClassLoaderHelper.fromClassNameToResourceName(""));
    }

    @Test
    public void testIsInJdkPackage() {
        Assertions.assertFalse(ClassLoaderHelper.isInJdkPackage("io.quarkus.runtime.Test"));
        Assertions.assertFalse(ClassLoaderHelper.isInJdkPackage("javax.validation.Validation"));
        Assertions.assertTrue(ClassLoaderHelper.isInJdkPackage("java.lang.String"));
        Assertions.assertTrue(ClassLoaderHelper.isInJdkPackage("jdk.internal.misc.Unsafe"));
        Assertions.assertTrue(ClassLoaderHelper.isInJdkPackage("sun.misc.SignalHandler"));
    }
}
