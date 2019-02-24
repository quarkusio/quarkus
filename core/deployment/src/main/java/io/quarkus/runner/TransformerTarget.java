package io.quarkus.runner;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;

public interface TransformerTarget {

    void setTransformers(Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions);

}
