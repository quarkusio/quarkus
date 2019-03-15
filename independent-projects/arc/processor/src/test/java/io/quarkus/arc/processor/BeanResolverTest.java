package io.quarkus.arc.processor;

import static org.junit.Assert.assertTrue;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.junit.Test;

public class BeanResolverTest {

    @Test
    public void testPrimitiveMatches() {
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.BOOLEAN, PrimitiveType.BOOLEAN));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.BOOLEAN,
                ClassType.create(DotName.createSimple(Boolean.class.getName()), Type.Kind.CLASS)));

        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.BYTE, PrimitiveType.BYTE));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.BYTE,
                ClassType.create(DotName.createSimple(Byte.class.getName()), Type.Kind.CLASS)));

        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.CHAR,
                ClassType.create(DotName.createSimple(Character.class.getName()), Type.Kind.CLASS)));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.CHAR, PrimitiveType.CHAR));

        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.DOUBLE, PrimitiveType.DOUBLE));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.DOUBLE,
                ClassType.create(DotName.createSimple(Double.class.getName()), Type.Kind.CLASS)));

        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.FLOAT, PrimitiveType.FLOAT));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.FLOAT,
                ClassType.create(DotName.createSimple(Float.class.getName()), Type.Kind.CLASS)));

        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.INT, PrimitiveType.INT));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.INT,
                ClassType.create(DotName.createSimple(Integer.class.getName()), Type.Kind.CLASS)));

        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.LONG, PrimitiveType.LONG));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.LONG,
                ClassType.create(DotName.createSimple(Long.class.getName()), Type.Kind.CLASS)));

        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.SHORT, PrimitiveType.SHORT));
        assertTrue(BeanResolver.primitiveMatch(PrimitiveType.Primitive.SHORT,
                ClassType.create(DotName.createSimple(Short.class.getName()), Type.Kind.CLASS)));
    }

}
