package io.quarkus.optaplanner.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.thoughtworks.xstream.converters.reflection.SerializableConverter")
final class Target_SerializableConverter {

    @Substitute
    public Object doUnmarshal(final Object result, final Target_HierarchicalStreamReader reader,
            final Target_UnmarshallingContext context) {
        return null;
    }

    @Substitute
    public void doMarshal(final Object source, final Target_HierarchicalStreamWriter writer,
            final Target_MarshallingContext context) {
    }
}

@TargetClass(className = "com.thoughtworks.xstream.io.HierarchicalStreamReader")
final class Target_HierarchicalStreamReader {

}

@TargetClass(className = "com.thoughtworks.xstream.converters.UnmarshallingContext")
final class Target_UnmarshallingContext {

}

@TargetClass(className = "com.thoughtworks.xstream.io.HierarchicalStreamWriter")
final class Target_HierarchicalStreamWriter {

}

@TargetClass(className = "com.thoughtworks.xstream.converters.MarshallingContext")
final class Target_MarshallingContext {

}

@TargetClass(className = "com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider")
final class Target_PureJavaReflectionProvider {

    @Substitute
    private Object instantiateUsingSerialization(final Class type) {
        return null;
    }
}

class XStreamSubstitutions {

}
