package io.quarkus.deployment.lambda;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.H_INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.H_INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.H_NEWINVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.This;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ExecutableCreator;
import io.quarkus.gizmo2.creator.ModifierFlag;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Rewrites the lambdas and method references of a class into ordinary classes generated at build time.
 * <p>
 * The JVM implements a lambda with an {@code invokedynamic} call site bootstrapped by
 * {@link java.lang.invoke.LambdaMetafactory}, which spins a hidden class the first time the call site is reached.
 * Spinning costs a few hundred microseconds per lambda, and the hidden class cannot come from the AOT cache unless
 * it was spun during the training run, on the same thread shape. For a class on the startup path, this rewrites each
 * such call site into a static call to a factory method of a generated class that implements the functional
 * interface, holds the captured arguments in fields and calls the implementation method, exactly as an anonymous
 * class would. The generated classes are regular classes of the application, loaded from the archive like any
 * other.
 * <p>
 * The implementation method of a lambda is a private synthetic method of the host class. The generated class lives
 * in the same package, so that method is made package-private (public for an interface host, whose methods cannot
 * be package-private). Call sites that cannot be rewritten are left untouched: serializable lambdas, and method
 * references to a method of a superclass ({@code super::method}).
 */
public final class LambdaMaterializer {

    private static final String METAFACTORY_OWNER = Type.getInternalName(LambdaMetafactory.class);

    /**
     * The result of a rewrite: the new bytes of the host class and the number of call sites rewritten. Zero means
     * nothing was rewritten and the host bytes are the original ones.
     */
    public record Result(byte[] hostClass, int callSites) {
    }

    private LambdaMaterializer() {
    }

    /**
     * Rewrites the lambda call sites of a class.
     *
     * @param classBytes the class to rewrite
     * @param output where the generated classes are written
     * @return the rewritten class and the number of call sites rewritten
     */
    public static Result materialize(byte[] classBytes, ClassOutput output) {
        ClassReader reader = new ClassReader(classBytes);
        Collector collector = new Collector();
        reader.accept(collector, ClassReader.SKIP_FRAMES);
        if (collector.sites.isEmpty()) {
            return new Result(classBytes, 0);
        }

        String host = reader.getClassName();
        boolean interfaceHost = (reader.getAccess() & ACC_INTERFACE) != 0;

        Gizmo gizmo = Gizmo.create(output)
                .withDebugInfo(false)
                .withParameters(false);
        Set<String> widened = new HashSet<>();
        int index = 0;
        for (Site site : collector.sites) {
            site.generatedName = host + "$QuarkusLambda$" + index++;
            if (site.impl.getOwner().equals(host) && collector.privateMethods.contains(key(site.impl))) {
                site.implIsPrivateHostMethod = true;
                widened.add(key(site.impl));
            }
            generate(gizmo, site, interfaceHost);
        }

        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new Rewriter(writer, collector.sites, widened, interfaceHost), 0);
        return new Result(writer.toByteArray(), collector.sites.size());
    }

    private static String key(Handle handle) {
        return handle.getName() + handle.getDesc();
    }

    /** A rewritable {@code invokedynamic} call site, identified by its position in the class. */
    private static final class Site {
        final String samName;
        final String indyDesc;
        final Type samType;
        final Handle impl;
        final Type instantiatedType;
        final List<Type> markers;
        final List<Type> bridges;
        String generatedName;
        boolean implIsPrivateHostMethod;

        Site(String samName, String indyDesc, Type samType, Handle impl, Type instantiatedType, List<Type> markers,
                List<Type> bridges) {
            this.samName = samName;
            this.indyDesc = indyDesc;
            this.samType = samType;
            this.impl = impl;
            this.instantiatedType = instantiatedType;
            this.markers = markers;
            this.bridges = bridges;
        }
    }

    /**
     * Reads a call site; null when it is not one this class rewrites. The position of the site within the class is
     * what pairs the two passes: the sites are collected and then rewritten in the same order.
     */
    private static Site site(String host, String name, String desc, Handle bsm, Object... args) {
        if (!bsm.getOwner().equals(METAFACTORY_OWNER)) {
            return null;
        }
        List<Type> markers = List.of();
        List<Type> bridges = List.of();
        if (bsm.getName().equals("altMetafactory")) {
            int flags = (Integer) args[3];
            if ((flags & LambdaMetafactory.FLAG_SERIALIZABLE) != 0) {
                return null;
            }
            int i = 4;
            if ((flags & LambdaMetafactory.FLAG_MARKERS) != 0) {
                int count = (Integer) args[i++];
                markers = new ArrayList<>(count);
                for (int j = 0; j < count; j++) {
                    markers.add((Type) args[i++]);
                }
            }
            if ((flags & LambdaMetafactory.FLAG_BRIDGES) != 0) {
                int count = (Integer) args[i++];
                bridges = new ArrayList<>(count);
                for (int j = 0; j < count; j++) {
                    bridges.add((Type) args[i++]);
                }
            }
        } else if (!bsm.getName().equals("metafactory")) {
            return null;
        }
        Handle impl = (Handle) args[1];
        if (impl.getTag() == H_INVOKESPECIAL && !impl.getOwner().equals(host)) {
            // super::method, which only the host itself may call
            return null;
        }
        return new Site(name, desc, (Type) args[0], impl, (Type) args[2], markers, bridges);
    }

    /** First pass: the call sites and the private methods of the host. */
    private static final class Collector extends ClassVisitor {
        final List<Site> sites = new ArrayList<>();
        final Set<String> privateMethods = new HashSet<>();
        String host;

        Collector() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            host = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            if ((access & ACC_PRIVATE) != 0) {
                privateMethods.add(name + descriptor);
            }
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... args) {
                    Site site = site(host, name, descriptor, bsm, args);
                    if (site != null) {
                        sites.add(site);
                    }
                }
            };
        }
    }

    /** Second pass: the call sites become static calls to the generated factories, the implementations open up. */
    private static final class Rewriter extends ClassVisitor {
        private final List<Site> sites;
        private final Set<String> widened;
        private final boolean interfaceHost;
        private String host;
        private int next;

        Rewriter(ClassVisitor cv, List<Site> sites, Set<String> widened, boolean interfaceHost) {
            super(Opcodes.ASM9, cv);
            this.sites = sites;
            this.widened = widened;
            this.interfaceHost = interfaceHost;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            host = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            if (widened.contains(name + descriptor)) {
                access &= ~ACC_PRIVATE;
                if (interfaceHost) {
                    access |= ACC_PUBLIC;
                }
            }
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... args) {
                    Site site = site(host, name, descriptor, bsm, args);
                    if (site == null) {
                        super.visitInvokeDynamicInsn(name, descriptor, bsm, args);
                        return;
                    }
                    Site generated = sites.get(next++);
                    super.visitMethodInsn(INVOKESTATIC, generated.generatedName, "create", descriptor, false);
                }
            };
        }
    }

    /**
     * The class standing for one call site: fields for the captured arguments, a constructor, a static factory
     * with the signature of the call site (which replaces it), the functional method and its bridges.
     */
    private static void generate(Gizmo gizmo, Site site, boolean interfaceHost) {
        ClassDesc self = ClassDesc.ofInternalName(site.generatedName);
        ClassDesc[] captured = descs(Type.getArgumentTypes(site.indyDesc));
        ClassDesc functionalInterface = desc(Type.getReturnType(site.indyDesc));
        MethodTypeDesc samType = MethodTypeDesc.ofDescriptor(site.samType.getDescriptor());

        gizmo.class_(self, cc -> {
            cc.packagePrivate();
            cc.final_();
            cc.synthetic();
            cc.implements_(functionalInterface);
            for (Type marker : site.markers) {
                cc.implements_(desc(marker));
            }
            This this_ = cc.this_();

            FieldDesc[] fields = new FieldDesc[captured.length];
            for (int i = 0; i < captured.length; i++) {
                ClassDesc type = captured[i];
                fields[i] = cc.field("arg$" + i, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(type);
                });
            }

            ConstructorDesc ctor = cc.constructor(MethodTypeDesc.of(ConstantDescs.CD_void, captured), con -> {
                con.private_();
                ParamVar[] params = parameters(con, captured);
                con.body(b -> {
                    b.invokeSpecial(ConstructorDesc.of(ConstantDescs.CD_Object), this_);
                    for (int i = 0; i < fields.length; i++) {
                        b.set(this_.field(fields[i]), params[i]);
                    }
                    b.return_();
                });
            });

            // the factory that replaces the call site, and a shared instance when nothing is captured, as the
            // metafactory does
            if (captured.length == 0) {
                StaticFieldVar instance = cc.staticField("INSTANCE", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(self);
                });
                cc.staticInitializer(b -> b.set(instance, b.new_(ctor, List.of())));
                cc.staticMethod("create", mc -> {
                    mc.packagePrivate();
                    mc.returning(functionalInterface);
                    mc.body(b -> b.return_(b.get(instance)));
                });
            } else {
                cc.staticMethod("create", mc -> {
                    mc.packagePrivate();
                    mc.returning(functionalInterface);
                    ParamVar[] params = parameters(mc, captured);
                    mc.body(b -> b.return_(b.new_(ctor, List.of(params))));
                });
            }

            // the functional method: the captured arguments, then its own, adapted to what the implementation
            // expects, then the result adapted to what the interface declares
            cc.method(site.samName, mc -> {
                mc.public_();
                mc.returning(samType.returnType());
                ParamVar[] params = parameters(mc, samType.parameterArray());
                mc.body(b -> {
                    List<Expr> args = new ArrayList<>(fields.length + params.length);
                    for (FieldDesc field : fields) {
                        args.add(this_.field(field));
                    }
                    Collections.addAll(args, params);
                    Expr result = invokeImplementation(b, site, interfaceHost, args);
                    return_(b, result, samType.returnType());
                });
            });

            // the bridges: the erasures the functional method is also reachable under
            for (Type bridge : site.bridges) {
                MethodTypeDesc bridgeType = MethodTypeDesc.ofDescriptor(bridge.getDescriptor());
                cc.method(site.samName, mc -> {
                    mc.public_();
                    mc.addFlag(ModifierFlag.BRIDGE);
                    mc.synthetic();
                    mc.returning(bridgeType.returnType());
                    ParamVar[] params = parameters(mc, bridgeType.parameterArray());
                    mc.body(b -> {
                        List<Expr> args = new ArrayList<>(params.length);
                        for (int i = 0; i < params.length; i++) {
                            args.add(adapt(b, params[i], samType.parameterType(i)));
                        }
                        Expr result = b.invokeVirtual(ClassMethodDesc.of(self, site.samName, samType), this_, args);
                        return_(b, result, bridgeType.returnType());
                    });
                });
            }
        });
    }

    private static ParamVar[] parameters(ExecutableCreator creator, ClassDesc[] types) {
        ParamVar[] params = new ParamVar[types.length];
        for (int i = 0; i < types.length; i++) {
            ClassDesc type = types[i];
            params[i] = creator.parameter("p" + i, pc -> pc.setType(type));
        }
        return params;
    }

    /** Calls the implementation method with the arguments adapted to its parameter types. */
    private static Expr invokeImplementation(BlockCreator b, Site site, boolean interfaceHost, List<Expr> args) {
        Handle impl = site.impl;
        ClassDesc owner = ClassDesc.ofInternalName(impl.getOwner());
        MethodTypeDesc implType = MethodTypeDesc.ofDescriptor(impl.getDesc());
        boolean itf = impl.isInterface();
        if (impl.getTag() == H_INVOKESPECIAL && site.implIsPrivateHostMethod) {
            // a private method of the host, now accessible: invoked like any other instance method
            itf = interfaceHost;
        }
        MethodDesc method = itf ? InterfaceMethodDesc.of(owner, impl.getName(), implType)
                : ClassMethodDesc.of(owner, impl.getName(), implType);

        List<Expr> adapted = new ArrayList<>(args.size());
        int next = 0;
        boolean instance = impl.getTag() != H_INVOKESTATIC && impl.getTag() != H_NEWINVOKESPECIAL;
        Expr receiver = null;
        if (instance) {
            receiver = adapt(b, args.get(next++), owner);
        }
        for (int i = 0; i < implType.parameterCount(); i++) {
            adapted.add(adapt(b, args.get(next++), implType.parameterType(i)));
        }

        return switch (impl.getTag()) {
            case H_NEWINVOKESPECIAL -> b.new_(ConstructorDesc.of(owner, implType), adapted);
            case H_INVOKESTATIC -> b.invokeStatic(method, adapted);
            case H_INVOKEINTERFACE -> b.invokeInterface(method, receiver, adapted);
            default -> itf ? b.invokeInterface(method, receiver, adapted) : b.invokeVirtual(method, receiver, adapted);
        };
    }

    private static void return_(BlockCreator b, Expr result, ClassDesc returnType) {
        if (returnType.equals(ConstantDescs.CD_void)) {
            // an unused value is dropped by the block
            b.return_();
        } else {
            b.return_(adapt(b, result, returnType));
        }
    }

    /**
     * Narrows a reference to the type the implementation expects. Every other adaptation of the metafactory,
     * boxing, unboxing, widening, and narrowing from {@code Object}, Gizmo applies itself to the arguments of
     * an invocation and to a returned value.
     */
    private static Expr adapt(BlockCreator b, Expr value, ClassDesc to) {
        ClassDesc from = value.type();
        if (from.equals(to) || from.isPrimitive() || to.isPrimitive() || to.equals(ConstantDescs.CD_Object)) {
            return value;
        }
        return b.cast(value, to);
    }

    private static ClassDesc desc(Type type) {
        return ClassDesc.ofDescriptor(type.getDescriptor());
    }

    private static ClassDesc[] descs(Type[] types) {
        ClassDesc[] descs = new ClassDesc[types.length];
        for (int i = 0; i < types.length; i++) {
            descs[i] = desc(types[i]);
        }
        return descs;
    }
}
