package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.deployment.Types.AssignabilityCheck;

public class TypesTest {

    @Test
    public void testIsAssignableFrom() throws IOException {
        Index index = index(String.class, CharSequence.class, BigDecimal.class, Number.class, Serializable.class, Boolean.class,
                Double.class);

        ClassType stringType = ClassType.create(DotName.createSimple(String.class));
        ClassType charSequenceType = ClassType.create(DotName.createSimple(CharSequence.class));
        ClassType bigDecimalType = ClassType.create(DotName.createSimple(BigDecimal.class));
        ClassType serializableType = ClassType.create(DotName.createSimple(Serializable.class));
        ClassType numberType = ClassType.create(DotName.createSimple(Number.class));
        Type booleanType = Types.box(Primitive.BOOLEAN);
        ArrayType byteArrayType = ArrayType.create(PrimitiveType.BYTE, 1);
        ArrayType intArrayType = ArrayType.create(PrimitiveType.INT, 1);
        AssignabilityCheck assignabilityCheck = new AssignabilityCheck(index);

        // byte[] is not assignable from String
        assertFalse(assignabilityCheck.isAssignableFrom(byteArrayType, stringType));
        // CharSequence is assignable from String
        assertTrue(assignabilityCheck.isAssignableFrom(charSequenceType, stringType));
        // String is not assignable from CharSequence
        assertFalse(assignabilityCheck.isAssignableFrom(stringType, charSequenceType));
        // String is not assignable from byte[]
        assertFalse(assignabilityCheck.isAssignableFrom(stringType, byteArrayType));
        // Object is assignable from any type
        assertTrue(assignabilityCheck.isAssignableFrom(ClassType.OBJECT_TYPE, stringType));
        // boolean is assignable from Boolean
        assertTrue(assignabilityCheck.isAssignableFrom(PrimitiveType.BOOLEAN, booleanType));
        // boolean is not assignable from double
        assertFalse(assignabilityCheck.isAssignableFrom(PrimitiveType.BOOLEAN, PrimitiveType.DOUBLE));
        // Serializable is assignable from BigDecimal
        assertTrue(assignabilityCheck.isAssignableFrom(serializableType, bigDecimalType));
        // Number is assignable from BigDecimal
        assertTrue(assignabilityCheck.isAssignableFrom(numberType, bigDecimalType));
        // byte[] is not assignable from int[]
        assertFalse(assignabilityCheck.isAssignableFrom(byteArrayType, intArrayType));
    }

    private static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = TypesTest.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

}
