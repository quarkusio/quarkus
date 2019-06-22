package io.quarkus.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReflectionAgent {

    private static volatile Set<String> KNOWN_CLASSES;

    private static final Set<String> warned = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void premain(java.lang.String s, java.lang.instrument.Instrumentation i) {
        i.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (className.startsWith("io/quarkus/agent")
                        || className.startsWith("java/")
                        || className.startsWith("javax/")
                        || className.startsWith("jdk/")
                        || className.startsWith("sun/")
                        || className.startsWith("org/jboss/log")) {
                    return null;
                }
                try {
                    AtomicBoolean modified = new AtomicBoolean(false);
                    ClassReader reader = new ClassReader(className);
                    ClassWriter writer = new ClassWriter(reader, Opcodes.ASM7);
                    reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                String[] exceptions) {
                            MethodVisitor existing = super.visitMethod(access, name, descriptor, signature, exceptions);
                            return new MethodVisitor(Opcodes.ASM7, existing) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                        boolean isInterface) {
                                    if (owner.equals("java/lang/Class") && name.equals("forName")) {
                                        modified.set(true);
                                        super.visitMethodInsn(opcode, ReflectionAgent.class.getName().replace(".", "/"), name,
                                                descriptor, isInterface);
                                    } else if (owner.equals("java/lang/ClassLoader") && name.equals("loadClass")) {
                                        modified.set(true);
                                        if (descriptor.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
                                            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                                    ReflectionAgent.class.getName().replace(".", "/"), name,
                                                    "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;", false);
                                        } else {
                                            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                                    ReflectionAgent.class.getName().replace(".", "/"), name,
                                                    "(Ljava/lang/ClassLoader;Ljava/lang/String;Z)Ljava/lang/Class;", false);
                                        }
                                    } else {
                                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                    }
                                }
                            };
                        }
                    }, 0);
                    if (modified.get()) {
                        return writer.toByteArray();
                    }
                    return null;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static Class<?> loadClass(ClassLoader loader, String className) throws ClassNotFoundException {
        handleClass(className);
        return loader.loadClass(className);
    }

    public static Class<?> loadClass(ClassLoader loader, String className, boolean resolve) throws ClassNotFoundException {
        handleClass(className);
        return loader.loadClass(className);
    }

    public static Class<?> forName(String className) throws ClassNotFoundException {
        handleClass(className);
        //this is not technically correct, for Quarkus we know there is only one class loader
        //once we are on JDK11 we can revisit
        return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
    }

    private static void handleClass(String className) {
        if (KNOWN_CLASSES == null) {
            Set<String> known = new HashSet<>();
            try (InputStream in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("META-INF/reflective-classes.txt")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    known.add(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            KNOWN_CLASSES = Collections.unmodifiableSet(known);
        }
        if (!KNOWN_CLASSES.contains(className)) {
            if (!warned.contains(className)) {
                boolean valid = true;
                for (StackTraceElement s : new RuntimeException().getStackTrace()) {
                    if (s.getMethodName().equals("<clinit>")) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    System.err.println("REFLECTIVE CLASS NOT REGISTERED: " + className);
                }
                warned.add(className);
            }
        }
    }

    public static Class<?> forName(String name, boolean initialize,
            ClassLoader loader) throws ClassNotFoundException {
        handleClass(name);
        return Class.forName(name, initialize, loader);
    }
}
