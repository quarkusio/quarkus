package io.quarkus.arc.processor;

import org.jboss.jandex.DotName;

public final class KotlinDotNames {
    public static final DotName METADATA = DotName.createSimple("kotlin.Metadata");
    public static final DotName UNIT = DotName.createSimple("kotlin.Unit");

    public static final DotName CONTINUATION = DotName.createSimple("kotlin.coroutines.Continuation");
}
