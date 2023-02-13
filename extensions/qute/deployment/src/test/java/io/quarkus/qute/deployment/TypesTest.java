package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.deployment.Types.AssignableInfo;

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
        Map<DotName, AssignableInfo> cache = new HashMap<>();

        // byte[] is not assignable from String
        assertFalse(Types.isAssignableFrom(byteArrayType, stringType, index, cache));
        // CharSequence is assignable from String
        assertTrue(Types.isAssignableFrom(charSequenceType, stringType, index, cache));
        // String is not assignable from CharSequence
        assertFalse(Types.isAssignableFrom(stringType, charSequenceType, index, cache));
        // String is not assignable from byte[]
        assertFalse(Types.isAssignableFrom(stringType, byteArrayType, index, cache));
        // Object is assignable from any type
        assertTrue(Types.isAssignableFrom(ClassType.OBJECT_TYPE, stringType, index, cache));
        // boolean is assignable from Boolean
        assertTrue(Types.isAssignableFrom(PrimitiveType.BOOLEAN, booleanType, index, cache));
        // boolean is not assignable from double
        assertFalse(Types.isAssignableFrom(PrimitiveType.BOOLEAN, PrimitiveType.DOUBLE, index, cache));
        // Serializable is assignable from BigDecimal
        assertTrue(Types.isAssignableFrom(serializableType, bigDecimalType, index, cache));
        // Number is assignable from BigDecimal
        assertTrue(Types.isAssignableFrom(numberType, bigDecimalType, index, cache));
        // byte[] is not assignable from int[]
        assertFalse(Types.isAssignableFrom(byteArrayType, intArrayType, index, cache));
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
