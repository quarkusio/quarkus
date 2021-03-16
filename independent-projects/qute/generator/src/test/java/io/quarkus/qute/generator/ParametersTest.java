package io.quarkus.qute.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.qute.generator.ExtensionMethodGenerator.ParamKind;
import io.quarkus.qute.generator.ExtensionMethodGenerator.Parameters;
import java.io.IOException;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;

public class ParametersTest {

    @Test
    public void testParameters() throws IOException {
        IndexView index = SimpleGeneratorTest.index(MyService.class);
        DotName myServiceName = DotName.createSimple(MyService.class.getName());
        ClassInfo myServiceClass = index.getClassByName(myServiceName);

        MethodInfo getDummy1 = myServiceClass.method("getDummy", Type.create(myServiceName, Kind.CLASS), PrimitiveType.INT,
                Type.create(DotNames.STRING, Kind.CLASS));
        Parameters params = new Parameters(getDummy1, false, false);
        assertEquals(3, params.size());
        assertEquals(2, params.evaluated().size());
        assertTrue(params.needsEvaluation());
        assertEquals(myServiceName, params.get(0).type.name());
        assertNotNull(params.getFirst(ParamKind.BASE));
        assertNull(params.getFirst(ParamKind.NAME));

        MethodInfo getDummyMatchAnyLike = myServiceClass.method("getDummyMatchAnyLike",
                Type.create(myServiceName, Kind.CLASS), Type.create(DotNames.STRING, Kind.CLASS), PrimitiveType.INT);
        params = new Parameters(getDummyMatchAnyLike, true, false);
        assertEquals(3, params.size());
        assertEquals(1, params.evaluated().size());
        assertTrue(params.needsEvaluation());
        assertNotNull(params.getFirst(ParamKind.BASE));
        assertNotNull(params.getFirst(ParamKind.NAME));

        MethodInfo getDummyNamespaceLike = myServiceClass.method("getDummyNamespaceLike",
                Type.create(DotNames.STRING, Kind.CLASS), Type.create(DotName.createSimple(int[].class.getName()), Kind.ARRAY));
        params = new Parameters(getDummyNamespaceLike, false, true);
        assertEquals(2, params.size());
        assertEquals(2, params.evaluated().size());
        assertTrue(params.needsEvaluation());
        assertNull(params.getFirst(ParamKind.BASE));
        assertNull(params.getFirst(ParamKind.NAME));

        MethodInfo getDummyNamespaceRegexLike = myServiceClass.method("getDummyNamespaceRegexLike",
                Type.create(DotNames.STRING, Kind.CLASS), Type.create(DotNames.STRING, Kind.CLASS));
        params = new Parameters(getDummyNamespaceRegexLike, true, true);
        assertEquals(2, params.size());
        assertEquals(1, params.evaluated().size());
        assertTrue(params.needsEvaluation());
        assertNull(params.getFirst(ParamKind.BASE));
        assertNotNull(params.getFirst(ParamKind.NAME));

        MethodInfo quark = myServiceClass.method("quark");
        params = new Parameters(quark, false, true);
        assertEquals(0, params.size());
        assertEquals(0, params.evaluated().size());
        assertFalse(params.needsEvaluation());
        assertNull(params.getFirst(ParamKind.BASE));
        assertNull(params.getFirst(ParamKind.NAME));
    }

}
