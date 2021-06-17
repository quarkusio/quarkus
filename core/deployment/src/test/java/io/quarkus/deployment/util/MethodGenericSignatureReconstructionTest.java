package io.quarkus.deployment.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;

public class MethodGenericSignatureReconstructionTest {

    private static final IndexView index = index(
            OuterRaw.class,
            OuterRaw.NestedRaw.class, OuterRaw.NestedParam.class, OuterRaw.NestedParamBound.class,
            OuterRaw.InnerRaw.class, OuterRaw.InnerParam.class, OuterRaw.InnerParamBound.class,
            OuterRaw.InnerParamBound.DoubleInner.class,
            OuterParam.class,
            OuterParam.NestedRaw.class, OuterParam.NestedParam.class, OuterParam.NestedParamBound.class,
            OuterParam.InnerRaw.class, OuterParam.InnerParam.class, OuterParam.InnerParamBound.class,
            OuterParam.InnerParamBound.DoubleInner.class,
            OuterParamBound.class,
            OuterParamBound.NestedRaw.class, OuterParamBound.NestedParam.class, OuterParamBound.NestedParamBound.class,
            OuterParamBound.InnerRaw.class, OuterParamBound.InnerParam.class, OuterParamBound.InnerParamBound.class,
            OuterParamBound.InnerParamBound.DoubleInner.class);

    // expected signatures here were obtained from classes compiled with OpenJDK 11.0.11
    // and then dumped using `javap -v`

    @Test
    public void test() {
        assertSignature(OuterRaw.class, "aaa", null);
        assertSignature(OuterRaw.class, "bbb",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(TU;Lio/quarkus/deployment/util/OuterRaw;)TT;");
        assertSignature(OuterRaw.NestedRaw.class, "ccc",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<+TU;>;Lio/quarkus/deployment/util/OuterRaw$NestedRaw;)TT;^Ljava/lang/IllegalArgumentException;^TV;");
        assertSignature(OuterRaw.NestedParam.class, "ddd",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<-TU;>;TX;Lio/quarkus/deployment/util/OuterRaw$NestedParam<TX;>;)TT;");
        assertSignature(OuterRaw.NestedParamBound.class, "eee",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TX;Lio/quarkus/deployment/util/OuterRaw$NestedParamBound<TX;>;)TT;^TV;");
        assertSignature(OuterRaw.InnerRaw.class, "fff",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<+TU;>;Lio/quarkus/deployment/util/OuterRaw$InnerRaw;)TT;^Ljava/lang/IllegalArgumentException;^TV;");
        assertSignature(OuterRaw.InnerParam.class, "ggg",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<-TU;>;TX;Lio/quarkus/deployment/util/OuterRaw$InnerParam<TX;>;)TT;");
        assertSignature(OuterRaw.InnerParamBound.class, "hhh",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TX;Lio/quarkus/deployment/util/OuterRaw$InnerParamBound<TX;>;)TT;^TV;");
        assertSignature(OuterRaw.InnerParamBound.DoubleInner.class, "iii",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TY;TX;Lio/quarkus/deployment/util/OuterRaw$InnerParamBound<TX;>.DoubleInner<TY;>;)TT;^TV;");

        assertSignature(OuterParam.class, "aaa",
                "(Ljava/lang/String;TW;Lio/quarkus/deployment/util/OuterParam<TW;>;)Ljava/lang/String;");
        assertSignature(OuterParam.class, "bbb",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(TU;TW;Lio/quarkus/deployment/util/OuterParam<TW;>;)TT;");
        assertSignature(OuterParam.NestedRaw.class, "ccc",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<+TU;>;Lio/quarkus/deployment/util/OuterParam$NestedRaw;)TT;^Ljava/lang/IllegalArgumentException;^TV;");
        assertSignature(OuterParam.NestedParam.class, "ddd",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<-TU;>;TX;Lio/quarkus/deployment/util/OuterParam$NestedParam<TX;>;)TT;");
        assertSignature(OuterParam.NestedParamBound.class, "eee",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TX;Lio/quarkus/deployment/util/OuterParam$NestedParamBound<TX;>;)TT;^TV;");
        assertSignature(OuterParam.InnerRaw.class, "fff",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<+TU;>;TW;Lio/quarkus/deployment/util/OuterParam<TW;>.InnerRaw;)TT;^Ljava/lang/IllegalArgumentException;^TV;");
        assertSignature(OuterParam.InnerParam.class, "ggg",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<-TU;>;TX;TW;Lio/quarkus/deployment/util/OuterParam<TW;>.InnerParam<TX;>;)TT;");
        assertSignature(OuterParam.InnerParamBound.class, "hhh",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TX;TW;Lio/quarkus/deployment/util/OuterParam<TW;>.InnerParamBound<TX;>;)TT;^TV;");
        assertSignature(OuterParam.InnerParamBound.DoubleInner.class, "iii",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TY;TX;TW;Lio/quarkus/deployment/util/OuterParam<TW;>.InnerParamBound<TX;>.DoubleInner<TY;>;)TT;^TV;");

        assertSignature(OuterParamBound.class, "aaa",
                "(Ljava/lang/String;TW;Lio/quarkus/deployment/util/OuterParamBound<TW;>;)Ljava/lang/String;");
        assertSignature(OuterParamBound.class, "bbb",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(TU;TW;Lio/quarkus/deployment/util/OuterParamBound<TW;>;)TT;");
        assertSignature(OuterParamBound.NestedRaw.class, "ccc",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<+TU;>;Lio/quarkus/deployment/util/OuterParamBound$NestedRaw;)TT;^Ljava/lang/IllegalArgumentException;^TV;");
        assertSignature(OuterParamBound.NestedParam.class, "ddd",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<-TU;>;TX;Lio/quarkus/deployment/util/OuterParamBound$NestedParam<TX;>;)TT;");
        assertSignature(OuterParamBound.NestedParamBound.class, "eee",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TX;Lio/quarkus/deployment/util/OuterParamBound$NestedParamBound<TX;>;)TT;^TV;");
        assertSignature(OuterParamBound.InnerRaw.class, "fff",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<+TU;>;TW;Lio/quarkus/deployment/util/OuterParamBound<TW;>.InnerRaw;)TT;^Ljava/lang/IllegalArgumentException;^TV;");
        assertSignature(OuterParamBound.InnerParam.class, "ggg",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<-TU;>;TX;TW;Lio/quarkus/deployment/util/OuterParamBound<TW;>.InnerParam<TX;>;)TT;");
        assertSignature(OuterParamBound.InnerParamBound.class, "hhh",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TX;TW;Lio/quarkus/deployment/util/OuterParamBound<TW;>.InnerParamBound<TX;>;)TT;^TV;");
        assertSignature(OuterParamBound.InnerParamBound.DoubleInner.class, "iii",
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable<TT;>;U::Ljava/lang/Comparable<TU;>;V:Ljava/lang/Exception;>(Ljava/util/List<*>;TY;TX;TW;Lio/quarkus/deployment/util/OuterParamBound<TW;>.InnerParamBound<TX;>.DoubleInner<TY;>;)TT;^TV;");
    }

    private static void assertSignature(Class<?> clazz, String method, String expectedSignature) {
        DotName name = DotName.createSimple(clazz.getName());
        for (MethodInfo methodInfo : index.getClassByName(name).methods()) {
            if (method.equals(methodInfo.name())) {
                String actualSignature = AsmUtil.getSignatureIfRequired(methodInfo);
                assertEquals(expectedSignature, actualSignature);
                return;
            }
        }

        fail("Couldn't find method " + clazz.getName() + "#" + method + " in test index");
    }

    private static Index index(Class<?>... classes) {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try {
                try (InputStream stream = MethodGenericSignatureReconstructionTest.class.getClassLoader()
                        .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                    indexer.index(stream);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return indexer.complete();
    }
}
