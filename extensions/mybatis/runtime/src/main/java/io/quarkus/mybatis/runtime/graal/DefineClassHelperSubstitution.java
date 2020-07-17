package io.quarkus.mybatis.runtime.graal;

import java.lang.invoke.MethodHandles;

import org.apache.ibatis.javassist.CannotCompileException;
import org.apache.ibatis.javassist.util.proxy.DefineClassHelper;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK8OrEarlier;

@TargetClass(value = DefineClassHelper.class, onlyWith = JDK8OrEarlier.class)
final public class DefineClassHelperSubstitution {

    @Substitute
    public static Class<?> toClass(MethodHandles.Lookup lookup, byte[] bcode) throws CannotCompileException {
        throw new CannotCompileException("Not support");
    }

    @Substitute
    static Class<?> toPublicClass(String className, byte[] bcode) throws CannotCompileException {
        throw new CannotCompileException("Not support");
    }

}
