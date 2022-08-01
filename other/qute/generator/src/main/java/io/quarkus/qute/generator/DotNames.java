package io.quarkus.qute.generator;

import java.util.concurrent.CompletionStage;
import org.jboss.jandex.DotName;

final class DotNames {

    static final DotName BOOLEAN = DotName.createSimple(Boolean.class.getName());
    static final DotName BYTE = DotName.createSimple(Byte.class.getName());
    static final DotName CHARACTER = DotName.createSimple(Character.class.getName());
    static final DotName DOUBLE = DotName.createSimple(Double.class.getName());
    static final DotName FLOAT = DotName.createSimple(Float.class.getName());
    static final DotName INTEGER = DotName.createSimple(Integer.class.getName());
    static final DotName LONG = DotName.createSimple(Long.class.getName());
    static final DotName SHORT = DotName.createSimple(Short.class.getName());
    static final DotName STRING = DotName.createSimple(String.class.getName());
    static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    static final DotName OBJECT = DotName.createSimple(Object.class.getName());

}
