package io.quarkus.qute.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.qute.generator.ValueResolverGenerator.MethodKey;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;

public class MethodKeyTest {

    @Test
    public void testSorting() throws IOException {
        IndexView index = SimpleGeneratorTest.index(MyService.class);
        ClassInfo myServiceClass = index.getClassByName(DotName.createSimple(MyService.class.getName()));
        MethodKey getName = new MethodKey(myServiceClass.method("getName"));
        MethodKey hasName = new MethodKey(myServiceClass.method("hasName"));
        MethodKey isActive = new MethodKey(myServiceClass.method("isActive"));
        MethodKey getList1 = new MethodKey(myServiceClass.method("getList", PrimitiveType.INT));
        MethodKey getList2 = new MethodKey(myServiceClass.method("getList", PrimitiveType.INT,
                Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS)));
        MethodKey getList3 = new MethodKey(
                myServiceClass.method("getList", Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS)));
        List<MethodKey> keys = Arrays.asList(hasName, getList3, getList2, getList1, isActive, getName);
        Collections.sort(keys);
        assertEquals("getList", keys.get(0).name);
        assertEquals(1, keys.get(0).params.size());
        assertEquals("int", keys.get(0).params.get(0).toString());
        assertEquals("getList", keys.get(1).name);
        assertEquals(1, keys.get(1).params.size());
        assertEquals("java.lang.String", keys.get(1).params.get(0).toString());
        assertEquals("getList", keys.get(2).name);
        assertEquals(2, keys.get(2).params.size());
        assertEquals("getName", keys.get(3).name);
        assertEquals("hasName", keys.get(4).name);
        assertEquals("isActive", keys.get(5).name);

    }

}
