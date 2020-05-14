package io.quarkus.grpc.runtime.graal;

import com.google.protobuf.GeneratedMessageV3;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@SuppressWarnings("unused")
@TargetClass(value = GeneratedMessageV3.class, innerClass = { "FieldAccessorTable", "RepeatedFieldAccessor" })
final class Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_RepeatedFieldAccessor {

    @Substitute
    static Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_RepeatedFieldAccessor_MethodInvoker tryGetMethodHandleInvoke(
            Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_RepeatedFieldAccessor_ReflectionInvoker accessor) {
        // For the usage of the reflection invoker (RepeatedFieldAccessor class)
        return accessor;
    }
}

@SuppressWarnings("unused")
@TargetClass(value = GeneratedMessageV3.class, innerClass = { "FieldAccessorTable", "SingularFieldAccessor" })
final class Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_SingularFieldAccessor {

    @Substitute
    static Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_SingularFieldAccessor_MethodInvoker tryGetMethodHandleInvoke(
            Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_SingularFieldAccessor_ReflectionInvoker accessor) {
        // For the usage of the reflection invoker (SingularFieldAccessor class)
        return accessor;
    }
}

@TargetClass(value = GeneratedMessageV3.class, innerClass = { "FieldAccessorTable", "RepeatedFieldAccessor", "MethodInvoker" })
interface Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_RepeatedFieldAccessor_MethodInvoker {
    // Provide access to the GeneratedMessageV3.FieldAccessorTable.RepeatedFieldAccessor.MethodInvoker interface
    // to another substitution
}

@TargetClass(value = GeneratedMessageV3.class, innerClass = { "FieldAccessorTable", "SingularFieldAccessor", "MethodInvoker" })
interface Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_SingularFieldAccessor_MethodInvoker {
    // Provide access to the GeneratedMessageV3.FieldAccessorTable.SingularFieldAccessor.MethodInvoker interface
    // to another substitution
}

@TargetClass(value = GeneratedMessageV3.class, innerClass = { "FieldAccessorTable", "RepeatedFieldAccessor",
        "ReflectionInvoker" })
final class Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_RepeatedFieldAccessor_ReflectionInvoker
        implements
        Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_RepeatedFieldAccessor_MethodInvoker {
    // Provide access to the ReflectionInvoker class from another substitution.
}

@TargetClass(value = GeneratedMessageV3.class, innerClass = { "FieldAccessorTable", "SingularFieldAccessor",
        "ReflectionInvoker" })
final class Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_SingularFieldAccessor_ReflectionInvoker
        implements
        Target_com_google_protobuf_GeneratedMessageV3_FieldAccessorTable_SingularFieldAccessor_MethodInvoker {
    // Provide access to the ReflectionInvoker class from another substitution.
}

@SuppressWarnings("unused")
class ProtobufSubstitutions {
}
