package io.quarkus.arc.processor.bcextensions;

import java.util.function.Predicate;

class MethodPredicates {
    static final Predicate<String> IS_METHOD = it -> !it.startsWith("<"); // no <init> nor <clinit>
    static final Predicate<String> IS_CONSTRUCTOR = it -> it.equals("<init>");
    static final Predicate<String> IS_METHOD_OR_CONSTRUCTOR = it -> !it.startsWith("<") || it.equals("<init>");

    static final Predicate<org.jboss.jandex.MethodInfo> IS_METHOD_JANDEX = it -> IS_METHOD.test(it.name());
    static final Predicate<org.jboss.jandex.MethodInfo> IS_CONSTRUCTOR_JANDEX = it -> IS_CONSTRUCTOR.test(it.name());
    static final Predicate<org.jboss.jandex.MethodInfo> IS_METHOD_OR_CONSTRUCTOR_JANDEX = it -> IS_METHOD_OR_CONSTRUCTOR
            .test(it.name());
}
