package io.quarkus.arc.processor;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.GenericSignature;
import org.jboss.jandex.Type;

public class AsmUtil {
    /**
     * Returns the Java bytecode generic signature of a hypothetical subclass of given {@code superClass},
     * extending the class's type as denoted by {@code superClassAsType}.
     *
     * For example, given this superclass:
     *
     * <pre>
     * {@code
     * public class Foo<R> extends Bar<R> implements List<String> {
     * }
     * }
     * </pre>
     *
     * this method will return <tt>&lt;R:Ljava/lang/Object;>LFoo&lt;TR;>;</tt>.
     * This is because the hypothetical subclass is considered to be declared like this:
     *
     * <pre>
     * {@code
     * public class MyGeneratedClass<R> extends Foo<R> {
     * }
     * }
     * </pre>
     *
     * {@code Bar} and {@code List} are ignored, as they are not part of the signature of the hypothetical subclass.
     *
     * @param superClass the superclass of the class you want to generate the signature for
     * @param superClassAsType the superclass type usage in the {@code extends} clause of the hypothetical subclass
     * @return generic signature for the subclass
     */
    public static String getGeneratedSubClassSignature(ClassInfo superClass, Type superClassAsType) {
        StringBuilder signature = new StringBuilder();
        GenericSignature.forTypeParameters(superClass.typeParameters(), GenericSignature.NO_SUBSTITUTION, signature);
        GenericSignature.forType(superClassAsType, GenericSignature.NO_SUBSTITUTION, signature);
        return signature.toString();
    }
}
