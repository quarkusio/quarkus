package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Expression;
import io.quarkus.qute.deployment.TypeInfos.Info;

public class TypeInfosTest {

    @Test
    public void testHintPattern() {
        assertHints("<loop-element>", "<loop-element>");
        assertHints("<set#10><loop-element>", "<set#10>", "<loop-element>");
        assertHints("<set#10><loop#4><any_other>", "<set#10>", "<loop#4>", "<any_other>");
        assertHints("<set#10>loop-element>", "<set#10>");
    }

    @Test
    public void testCreate() throws IOException {
        List<Expression> expressions = Engine.builder().build()
                .parse("{@io.quarkus.qute.deployment.TypeInfosTest$Foo foo}{config:['foo.bar.baz']}{foo.name}")
                .getExpressions();
        ;
        IndexView index = index(Foo.class);

        List<Info> infos = TypeInfos.create(expressions.get(0), index, id -> "dummy");
        assertEquals(1, infos.size());
        assertTrue(infos.get(0).isProperty());
        assertEquals("foo.bar.baz", infos.get(0).value);

        infos = TypeInfos.create(expressions.get(1), index, id -> "dummy");
        assertEquals(2, infos.size());
        assertTrue(infos.get(0).isTypeInfo());
        assertEquals("io.quarkus.qute.deployment.TypeInfosTest$Foo", infos.get(0).asTypeInfo().rawClass.name().toString());
        assertTrue(infos.get(1).isProperty());
        assertEquals("name", infos.get(1).value);
    }

    private void assertHints(String hintStr, String... expectedHints) {
        Matcher m = TypeInfos.HintInfo.HINT_PATTERN.matcher(hintStr);
        List<String> hints = new ArrayList<>();
        while (m.find()) {
            hints.add(m.group());
        }
        assertEquals(Arrays.asList(expectedHints), hints);
    }

    private static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = TypeInfosTest.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

    public static class Foo {

        public String name;

    }

}
