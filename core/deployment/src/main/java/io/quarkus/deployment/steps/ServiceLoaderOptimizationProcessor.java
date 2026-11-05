package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.ServiceLoaderToOptimizeBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.Gizmo;

/**
 * Optimizes {@link java.util.ServiceLoader} usage by resolving service providers at build time
 * and rewriting call sites to avoid runtime classpath scanning.
 * <p>
 * For each {@link ServiceLoaderToOptimizeBuildItem}, this processor:
 * <ol>
 * <li>Reads the target class bytecode and finds {@code ServiceLoader.load()} call sites</li>
 * <li>Extracts the service interface from the {@code LDC} instruction</li>
 * <li>Resolves providers via {@link ServiceUtil}</li>
 * <li>Rewrites the call site to use {@link io.quarkus.runtime.util.ServiceLoaderUtil} instead</li>
 * </ol>
 */
public class ServiceLoaderOptimizationProcessor {

    private static final Logger log = Logger.getLogger(ServiceLoaderOptimizationProcessor.class);

    private static final String SERVICE_LOADER_INTERNAL = "java/util/ServiceLoader";
    private static final String ITERABLE_INTERNAL = "java/lang/Iterable";
    private static final String SERVICE_LOADER_UTIL_INTERNAL = "io/quarkus/runtime/util/ServiceLoaderUtil";

    private static final String LOAD_2_DESC = "(Ljava/lang/Class;Ljava/lang/ClassLoader;)Ljava/util/ServiceLoader;";
    private static final String LOAD_1_DESC = "(Ljava/lang/Class;)Ljava/util/ServiceLoader;";

    @BuildStep
    void optimize(List<ServiceLoaderToOptimizeBuildItem> items,
            BuildProducer<BytecodeTransformerBuildItem> transformers) {

        if (items.isEmpty()) {
            return;
        }

        ClassLoader deploymentCl = Thread.currentThread().getContextClassLoader();

        for (ServiceLoaderToOptimizeBuildItem item : items) {
            String className = item.getClassToTransform();
            String resourcePath = className.replace('.', '/') + ".class";

            ClassNode classNode;
            try (InputStream is = deploymentCl.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    log.warnf("Cannot find class %s for ServiceLoader optimization", className);
                    continue;
                }
                ClassReader cr = new ClassReader(is);
                classNode = new ClassNode();
                cr.accept(classNode, 0);
            } catch (IOException e) {
                log.warnf("Failed to read class %s for ServiceLoader optimization: %s", className, e.getMessage());
                continue;
            }

            Map<String, List<String>> serviceToProviders = new HashMap<>();
            Map<String, List<ServiceLoaderCallSite>> methodCallSites = new HashMap<>();

            for (MethodNode mn : classNode.methods) {
                List<ServiceLoaderCallSite> callSites = analyzeMethod(mn, deploymentCl, serviceToProviders);
                if (!callSites.isEmpty()) {
                    methodCallSites.put(mn.name + mn.desc, callSites);
                }
            }

            if (methodCallSites.isEmpty()) {
                log.debugf("No optimizable ServiceLoader.load() calls found in %s", className);
                continue;
            }

            transformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(className)
                    .setVisitorFunction((ignored, visitor) -> new ServiceLoaderRewriteVisitor(
                            visitor, methodCallSites, serviceToProviders))
                    .build());

            log.debugf("Optimized ServiceLoader.load() in %s for services: %s",
                    className, serviceToProviders.keySet());
        }
    }

    private List<ServiceLoaderCallSite> analyzeMethod(MethodNode mn, ClassLoader deploymentCl,
            Map<String, List<String>> serviceToProviders) {

        List<ServiceLoaderCallSite> result = new ArrayList<>();

        for (int i = 0; i < mn.instructions.size(); i++) {
            AbstractInsnNode insn = mn.instructions.get(i);
            if (!(insn instanceof MethodInsnNode mi)) {
                continue;
            }
            if (!SERVICE_LOADER_INTERNAL.equals(mi.owner)) {
                continue;
            }
            if (!"load".equals(mi.name)) {
                continue;
            }

            ServiceLoaderOverload overload = classifyOverload(mi);
            if (overload == null) {
                continue;
            }

            String serviceInternal = extractServiceClass(mn, i);
            if (serviceInternal == null) {
                log.debugf(
                        "ServiceLoader optimization: could not extract service class for ServiceLoader.load() in %s.%s, leaving original call",
                        mn.name, mn.desc);
                continue;
            }

            String serviceDotName = serviceInternal.replace('/', '.');
            if (!serviceToProviders.containsKey(serviceInternal)) {
                try {
                    Set<String> providers = ServiceUtil.classNamesNamedIn(deploymentCl,
                            "META-INF/services/" + serviceDotName);
                    serviceToProviders.put(serviceInternal, new ArrayList<>(providers));
                } catch (IOException e) {
                    serviceToProviders.put(serviceInternal, List.of());
                }
            }

            int storeSlot = findStoreSlot(mn, i);
            if (storeSlot >= 0 && !isOnlyUsedAsIterable(mn, i, storeSlot)) {
                log.debugf(
                        "ServiceLoader optimization: ServiceLoader.load(%s) result in %s.%s is used for ServiceLoader-specific methods, leaving original call",
                        serviceDotName, mn.name, mn.desc);
                continue;
            }

            List<String> providers = serviceToProviders.get(serviceInternal);
            if (providers.isEmpty()) {
                log.debugf(
                        "ServiceLoader optimization: ServiceLoader.load(%s) in %s.%s will be replaced with empty Iterable (no providers found)",
                        serviceDotName, mn.name, mn.desc);
            } else {
                log.debugf(
                        "ServiceLoader optimization: ServiceLoader.load(%s) in %s.%s will be replaced with direct instantiation of %s",
                        serviceDotName, mn.name, mn.desc, providers);
            }

            result.add(new ServiceLoaderCallSite(serviceInternal, overload, storeSlot));
        }

        return result;
    }

    private ServiceLoaderOverload classifyOverload(MethodInsnNode mi) {
        if (LOAD_2_DESC.equals(mi.desc)) {
            return ServiceLoaderOverload.LOAD_CLASS_CLASSLOADER;
        } else if (LOAD_1_DESC.equals(mi.desc)) {
            return ServiceLoaderOverload.LOAD_CLASS;
        }
        return null;
    }

    /**
     * Extracts the service interface class from the bytecode preceding a {@code ServiceLoader.load()} call.
     * <p>
     * The {@code Class} parameter is always the first argument pushed onto the stack. For {@code load(Class)},
     * it is the only argument so the nearest LDC Type is correct. For {@code load(Class, ClassLoader)},
     * we must skip past the ClassLoader argument (which may itself involve LDC Type instructions, e.g.
     * {@code LDC SomeClass.class; INVOKEVIRTUAL Class.getClassLoader()}) to find the service class.
     */
    private String extractServiceClass(MethodNode mn, int callSiteIndex) {
        MethodInsnNode loadInsn = (MethodInsnNode) mn.instructions.get(callSiteIndex);
        int argsToSkip = Type.getArgumentTypes(loadInsn.desc).length - 1;

        // Walk backward, skipping past the extra arguments to find the Class (first) argument.
        // When walking backward, each instruction's stack delta tells us how many values it
        // produced (+) or consumed (-). We accumulate until we've accounted for one full value
        // (stackDepth reaches +1), meaning we've walked past one argument's production chain.
        int stackDepth = 0;
        for (int j = callSiteIndex - 1; j >= 0; j--) {
            AbstractInsnNode insn = mn.instructions.get(j);
            if (insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LINE
                    || insn.getType() == AbstractInsnNode.FRAME) {
                continue;
            }

            if (argsToSkip > 0) {
                stackDepth += getStackDelta(insn);
                if (stackDepth >= 1) {
                    argsToSkip--;
                    stackDepth = 0;
                }
                continue;
            }

            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type
                    && type.getSort() == Type.OBJECT) {
                return type.getInternalName();
            }
            return null;
        }

        return null;
    }

    private int getStackDelta(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode mi) {
            int pops = Type.getArgumentTypes(mi.desc).length;
            if (mi.getOpcode() != Opcodes.INVOKESTATIC) {
                pops++;
            }
            int pushes = Type.getReturnType(mi.desc).getSort() == Type.VOID ? 0 : 1;
            return pushes - pops;
        }

        int opcode = insn.getOpcode();
        if (opcode == Opcodes.DUP) {
            return 1;
        }
        if (opcode == Opcodes.POP) {
            return -1;
        }
        if (opcode == Opcodes.SWAP) {
            return 0;
        }
        if (insn instanceof LdcInsnNode || insn instanceof VarInsnNode) {
            return 1;
        }
        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
            return 1;
        }
        if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
            return 1;
        }
        if (opcode == Opcodes.NEW || opcode == Opcodes.GETSTATIC) {
            return 1;
        }
        return 0;
    }

    private int findStoreSlot(MethodNode mn, int callSiteIndex) {
        for (int j = callSiteIndex + 1; j < mn.instructions.size(); j++) {
            AbstractInsnNode next = mn.instructions.get(j);
            if (next.getType() == AbstractInsnNode.LABEL || next.getType() == AbstractInsnNode.LINE
                    || next.getType() == AbstractInsnNode.FRAME) {
                continue;
            }
            if (next instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ASTORE) {
                return varInsn.var;
            }
            break;
        }
        return -1;
    }

    private boolean isOnlyUsedAsIterable(MethodNode mn, int callSiteIndex, int slot) {
        for (int j = callSiteIndex + 1; j < mn.instructions.size(); j++) {
            AbstractInsnNode insn = mn.instructions.get(j);
            if (insn instanceof VarInsnNode varInsn && varInsn.getOpcode() == Opcodes.ALOAD && varInsn.var == slot) {
                AbstractInsnNode next = findNextRealInsn(mn, j);
                if (next instanceof MethodInsnNode mi && SERVICE_LOADER_INTERNAL.equals(mi.owner)) {
                    if (!"iterator".equals(mi.name) && !"forEach".equals(mi.name)
                            && !"spliterator".equals(mi.name)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private AbstractInsnNode findNextRealInsn(MethodNode mn, int fromIndex) {
        for (int j = fromIndex + 1; j < mn.instructions.size(); j++) {
            AbstractInsnNode insn = mn.instructions.get(j);
            if (insn.getType() != AbstractInsnNode.LABEL && insn.getType() != AbstractInsnNode.LINE
                    && insn.getType() != AbstractInsnNode.FRAME) {
                return insn;
            }
        }
        return null;
    }

    private static class ServiceLoaderRewriteVisitor extends ClassVisitor {

        private final Map<String, List<ServiceLoaderCallSite>> methodCallSites;
        private final Map<String, List<String>> serviceToProviders;

        ServiceLoaderRewriteVisitor(ClassVisitor delegate,
                Map<String, List<ServiceLoaderCallSite>> methodCallSites,
                Map<String, List<String>> serviceToProviders) {
            super(Gizmo.ASM_API_VERSION, delegate);
            this.methodCallSites = methodCallSites;
            this.serviceToProviders = serviceToProviders;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            String key = name + descriptor;
            List<ServiceLoaderCallSite> callSites = methodCallSites.get(key);
            if (callSites != null && !callSites.isEmpty()) {
                return new ServiceLoaderMethodRewriter(mv, callSites, serviceToProviders);
            }
            return mv;
        }
    }

    private static class ServiceLoaderMethodRewriter extends MethodVisitor {

        private final List<ServiceLoaderCallSite> callSites;
        private final Map<String, List<String>> serviceToProviders;

        private int serviceLoaderCallIndex = 0;

        ServiceLoaderMethodRewriter(MethodVisitor delegate,
                List<ServiceLoaderCallSite> callSites,
                Map<String, List<String>> serviceToProviders) {
            super(Gizmo.ASM_API_VERSION, delegate);
            this.callSites = callSites;
            this.serviceToProviders = serviceToProviders;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                boolean isInterface) {

            if (opcode == Opcodes.INVOKESTATIC && SERVICE_LOADER_INTERNAL.equals(owner)
                    && "load".equals(name)
                    && serviceLoaderCallIndex < callSites.size()) {

                ServiceLoaderCallSite callSite = callSites.get(serviceLoaderCallIndex);
                ServiceLoaderOverload expectedOverload = classifyOverloadFromDesc(descriptor);

                if (expectedOverload == callSite.overload) {
                    List<String> providers = serviceToProviders.get(callSite.serviceInternal);
                    if (providers != null) {
                        serviceLoaderCallIndex++;
                        emitResolverCall(callSite.overload, providers);
                        return;
                    }
                }
            }

            // Replace ServiceLoader.iterator() with Iterable.iterator()
            if (opcode == Opcodes.INVOKEVIRTUAL && SERVICE_LOADER_INTERNAL.equals(owner)
                    && "iterator".equals(name)) {
                super.visitMethodInsn(Opcodes.INVOKEINTERFACE, ITERABLE_INTERNAL,
                        "iterator", "()Ljava/util/Iterator;", true);
                return;
            }

            // Replace ServiceLoader.forEach() with Iterable.forEach()
            if (opcode == Opcodes.INVOKEVIRTUAL && SERVICE_LOADER_INTERNAL.equals(owner)
                    && "forEach".equals(name)) {
                super.visitMethodInsn(Opcodes.INVOKEINTERFACE, ITERABLE_INTERNAL,
                        "forEach", "(Ljava/util/function/Consumer;)V", true);
                return;
            }

            // Replace ServiceLoader.spliterator() with Iterable.spliterator()
            if (opcode == Opcodes.INVOKEVIRTUAL && SERVICE_LOADER_INTERNAL.equals(owner)
                    && "spliterator".equals(name)) {
                super.visitMethodInsn(Opcodes.INVOKEINTERFACE, ITERABLE_INTERNAL,
                        "spliterator", "()Ljava/util/Spliterator;", true);
                return;
            }

            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        /**
         * Emits the call to {@code ServiceLoaderUtil.load()}, handling the stack manipulation
         * needed to replace the {@code ServiceLoader.load()} arguments.
         * <p>
         * For the 2-arg overload ({@code load(Class, ClassLoader)}), the Class is popped and replaced
         * with the provider name(s), while the ClassLoader is kept and passed through.
         * <p>
         * For the 1-arg overload ({@code load(Class)}), the Class is popped and replaced with
         * the provider name(s); the TCCL resolution is handled inside {@code ServiceLoaderUtil}.
         */
        private void emitResolverCall(ServiceLoaderOverload overload, List<String> providers) {
            if (overload == ServiceLoaderOverload.LOAD_CLASS_CLASSLOADER) {
                // Stack: [..., Class, ClassLoader]
                // SWAP + POP to discard Class, keeping ClassLoader
                super.visitInsn(Opcodes.SWAP);
                super.visitInsn(Opcodes.POP);
                // Stack: [..., ClassLoader]
                emitStringArray(providers);
                super.visitInsn(Opcodes.SWAP);
                // Stack: [..., String[], ClassLoader]
                super.visitMethodInsn(Opcodes.INVOKESTATIC, SERVICE_LOADER_UTIL_INTERNAL,
                        "load", "([Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Iterable;", false);
            } else {
                // Stack: [..., Class]
                super.visitInsn(Opcodes.POP);
                emitStringArray(providers);
                // Stack: [..., String[]]
                super.visitMethodInsn(Opcodes.INVOKESTATIC, SERVICE_LOADER_UTIL_INTERNAL,
                        "load", "([Ljava/lang/String;)Ljava/lang/Iterable;", false);
            }
        }

        private void emitStringArray(List<String> values) {
            super.visitLdcInsn(values.size());
            super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
            for (int i = 0; i < values.size(); i++) {
                super.visitInsn(Opcodes.DUP);
                super.visitLdcInsn(i);
                super.visitLdcInsn(values.get(i));
                super.visitInsn(Opcodes.AASTORE);
            }
        }

        private static ServiceLoaderOverload classifyOverloadFromDesc(String descriptor) {
            if (LOAD_2_DESC.equals(descriptor)) {
                return ServiceLoaderOverload.LOAD_CLASS_CLASSLOADER;
            } else if (LOAD_1_DESC.equals(descriptor)) {
                return ServiceLoaderOverload.LOAD_CLASS;
            }
            return null;
        }
    }

    private enum ServiceLoaderOverload {
        LOAD_CLASS_CLASSLOADER,
        LOAD_CLASS
    }

    private record ServiceLoaderCallSite(
            String serviceInternal,
            ServiceLoaderOverload overload,
            int storeSlot) {
    }
}
