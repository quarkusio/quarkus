package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class AsmUtilCopyTest {

    @Test
    public void testNeedsSignature() throws IOException {
        assertFalse(AsmUtilCopy.needsSignature(Basics.index(Object.class).getClassByName(DotNames.OBJECT)));
    }

}
