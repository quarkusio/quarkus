package io.quarkus.optaplanner.nativeimage;

import java.lang.reflect.Method;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.drools.core.rule.builder.dialect.asm.ClassGenerator")
final class Target_org_drools_core_rule_builder_dialect_asm_ClassGenerator {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset, name = "defineClassMethod")
    private static Method defineClassMethod;
}