package io.quarkus.commons.classloading;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassloadHelperTest {

    @Test
    public void testFromClassToResourceName() {
        Assertions.assertEquals("java/lang/String.class", ClassloadHelper.fromClassNameToResourceName("java.lang.String"));
        Assertions.assertEquals(".class", ClassloadHelper.fromClassNameToResourceName(""));
    }

}
