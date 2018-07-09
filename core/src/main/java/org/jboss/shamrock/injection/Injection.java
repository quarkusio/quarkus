package org.jboss.shamrock.injection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.classfilewriter.AccessFlag;
import org.jboss.classfilewriter.ClassFile;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.classfilewriter.code.CodeAttribute;
import org.jboss.shamrock.core.ClassOutput;


public class Injection {

    private static final AtomicInteger COUNT = new AtomicInteger();

    private static final Map<String, String> CACHE = new HashMap<>();

    public static String getInstanceFactoryName(String instanceType, ClassOutput classOutput) {
        if (CACHE.containsKey(instanceType)) {
            return CACHE.get(instanceType);
        }
        String name = Injection.class.getName() + "$$" + COUNT.incrementAndGet();
        ClassFile classFile = new ClassFile(name, AccessFlag.PUBLIC, "java.lang.Object", InjectionInstance.class.getName());


        ClassMethod method = classFile.addMethod(AccessFlag.PUBLIC, "newInstance", "Ljava/lang/Object;");
        CodeAttribute ca = method.getCodeAttribute();
        ca.newInstruction(instanceType);
        ca.dup();
        ca.invokespecial(instanceType, "<init>", "()V");
        ca.returnInstruction();


        ClassMethod ctor = classFile.addMethod(AccessFlag.PUBLIC, "<init>", "V");
        ca = ctor.getCodeAttribute();
        ca.aload(0);
        ca.invokespecial(Object.class.getName(), "<init>", "()V");
        ca.returnInstruction();

        try {
            classOutput.writeClass(name, classFile.toBytecode());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CACHE.put(instanceType, name);
        return name;
    }

}
