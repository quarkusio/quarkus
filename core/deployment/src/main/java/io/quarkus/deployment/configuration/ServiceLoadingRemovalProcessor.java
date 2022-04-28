package io.quarkus.deployment.configuration;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValidator;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

public class ServiceLoadingRemovalProcessor {

    private static final String CTOR_NAME = "<init>";

    private static final String ARRAYLIST_BINARY_NAME = "java/util/ArrayList";
    private static final String LIST_BINARY_NAME = "java/util/List";

    private static final String CONFIG_VALIDATOR_CLASS_NAME = ConfigValidator.class.getName();
    private static final String CONFIG_VALIDATOR_BINARY_NAME = CONFIG_VALIDATOR_CLASS_NAME.replace('.', '/');

    private static final String CONFIG_SOURCE_INTERCEPTOR_CLASS_NAME = ConfigSourceInterceptor.class.getName();
    private static final String CONFIG_SOURCE_INTERCEPTOR_BINARY_NAME = CONFIG_SOURCE_INTERCEPTOR_CLASS_NAME.replace('.', '/');

    private static final String CONFIG_SOURCE_INTERCEPTOR_FACTORY_CLASS_NAME = ConfigSourceInterceptorFactory.class.getName();
    private static final String CONFIG_SOURCE_INTERCEPTOR_FACTORY_BINARY_NAME = CONFIG_SOURCE_INTERCEPTOR_FACTORY_CLASS_NAME
            .replace('.', '/');

    private static final String INTERCEPTOR_WITH_PRIORITY_CLASS_NAME = "io/smallrye/config/SmallRyeConfigBuilder$InterceptorWithPriority";
    private static final String INTERCEPTOR_WITH_PRIORITY_BINARY_NAME = INTERCEPTOR_WITH_PRIORITY_CLASS_NAME.replace('.', '/');

    private static final String CONVERTER_CLASS_NAME = Converter.class.getName();
    private static final String CONVERTER_BINARY_NAME = CONVERTER_CLASS_NAME
            .replace('.', '/');

    // only done in normal mode since there is not much benefit for tests and dev mode
    // moreover, by doing it only for normal mode, we can instantiate the classes without
    // having to using reflection and the TCCL
    @BuildStep(onlyIf = IsNormal.class)
    public void transformSmallRyeConfigBuilder(List<ServiceProviderBuildItem> servicesItems,
            BuildProducer<BytecodeTransformerBuildItem> producer) {

        Services services = determineValidatorImplementation(servicesItems);
        producer.produce(new BytecodeTransformerBuildItem(SmallRyeConfigBuilder.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);

                                if (name.equals("discoverValidator")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            if (services.validator == null) {
                                                visitFieldInsn(Opcodes.GETSTATIC, CONFIG_VALIDATOR_BINARY_NAME, "EMPTY",
                                                        "L" + CONFIG_VALIDATOR_BINARY_NAME + ";");
                                                visitInsn(Opcodes.ARETURN);
                                            } else {
                                                String validatorImplBinaryName = services.validator.replace('.', '/');
                                                visitTypeInsn(NEW, validatorImplBinaryName);
                                                visitInsn(Opcodes.DUP);
                                                visitMethodInsn(Opcodes.INVOKESPECIAL, validatorImplBinaryName,
                                                        CTOR_NAME, "()V",
                                                        false);
                                                visitInsn(Opcodes.ARETURN);
                                            }

                                        }
                                    };
                                } else if (name.equals("discoverInterceptors")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            if (services.interceptorFactoryImpls.isEmpty()
                                                    && services.interceptorImpls.isEmpty()) {
                                                visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList",
                                                        "()Ljava/util/List;",
                                                        false);
                                                visitInsn(Opcodes.ARETURN);
                                            } else {
                                                visitTypeInsn(NEW, ARRAYLIST_BINARY_NAME);
                                                visitInsn(DUP);
                                                visitIntInsn(BIPUSH,
                                                        services.interceptorImpls.size()
                                                                + services.interceptorFactoryImpls.size());
                                                visitMethodInsn(INVOKESPECIAL, ARRAYLIST_BINARY_NAME, CTOR_NAME, "(I)V", false);
                                                visitVarInsn(ASTORE, 1);
                                                visitVarInsn(ALOAD, 1);
                                                for (String interceptorImpl : services.interceptorImpls) {
                                                    String binaryName = interceptorImpl.replace('.', '/');
                                                    visitTypeInsn(NEW, INTERCEPTOR_WITH_PRIORITY_BINARY_NAME);
                                                    visitInsn(DUP);
                                                    visitTypeInsn(NEW, binaryName);
                                                    visitInsn(DUP);
                                                    visitMethodInsn(INVOKESPECIAL,
                                                            binaryName, CTOR_NAME, "()V", false);
                                                    visitMethodInsn(INVOKESPECIAL,
                                                            INTERCEPTOR_WITH_PRIORITY_BINARY_NAME, CTOR_NAME,
                                                            "(L" + CONFIG_SOURCE_INTERCEPTOR_BINARY_NAME + ";)V", false);
                                                    visitMethodInsn(INVOKEINTERFACE, LIST_BINARY_NAME, "add",
                                                            "(Ljava/lang/Object;)Z", true);
                                                    visitInsn(POP);
                                                    visitVarInsn(ALOAD, 1);
                                                }
                                                for (String interceptorFactoryImpl : services.interceptorFactoryImpls) {
                                                    String binaryName = interceptorFactoryImpl.replace('.', '/');
                                                    visitTypeInsn(NEW, INTERCEPTOR_WITH_PRIORITY_BINARY_NAME);
                                                    visitInsn(DUP);
                                                    visitTypeInsn(NEW, binaryName);
                                                    visitInsn(DUP);
                                                    visitMethodInsn(INVOKESPECIAL,
                                                            binaryName, CTOR_NAME, "()V", false);
                                                    visitMethodInsn(INVOKESPECIAL,
                                                            INTERCEPTOR_WITH_PRIORITY_BINARY_NAME, CTOR_NAME,
                                                            "(L" + CONFIG_SOURCE_INTERCEPTOR_FACTORY_BINARY_NAME + ";)V",
                                                            false);
                                                    visitMethodInsn(INVOKEINTERFACE, LIST_BINARY_NAME, "add",
                                                            "(Ljava/lang/Object;)Z", true);
                                                    visitInsn(POP);
                                                    visitVarInsn(ALOAD, 1);
                                                }
                                                visitInsn(ARETURN);
                                            }
                                        }
                                    };
                                } else if (name.equals("discoverConverters")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            if (services.converters.isEmpty()) {
                                                visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList",
                                                        "()Ljava/util/List;",
                                                        false);
                                                visitInsn(Opcodes.ARETURN);
                                            } else {
                                                visitTypeInsn(NEW, ARRAYLIST_BINARY_NAME);
                                                visitInsn(DUP);
                                                visitIntInsn(BIPUSH,
                                                        services.converters.size());
                                                visitMethodInsn(INVOKESPECIAL, ARRAYLIST_BINARY_NAME, CTOR_NAME, "(I)V", false);
                                                visitVarInsn(ASTORE, 1);
                                                visitVarInsn(ALOAD, 1);
                                                for (String converterImpl : services.converters) {
                                                    String binaryName = converterImpl.replace('.', '/');
                                                    visitTypeInsn(NEW, binaryName);
                                                    visitInsn(DUP);
                                                    visitMethodInsn(INVOKESPECIAL,
                                                            binaryName, CTOR_NAME, "()V", false);
                                                    visitMethodInsn(INVOKEINTERFACE, LIST_BINARY_NAME, "add",
                                                            "(Ljava/lang/Object;)Z", true);
                                                    visitInsn(POP);
                                                    visitVarInsn(ALOAD, 1);
                                                }
                                                visitInsn(ARETURN);
                                            }
                                        }
                                    };
                                }

                                return original;
                            }
                        };
                    }
                }));

        producer.produce(new BytecodeTransformerBuildItem(ConfigProviderResolver.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("loadSpi")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            String binaryName = SmallRyeConfigProviderResolver.class.getName().replace('.',
                                                    '/');
                                            visitTypeInsn(NEW, binaryName);
                                            visitInsn(DUP);
                                            visitMethodInsn(INVOKESPECIAL,
                                                    binaryName, CTOR_NAME, "()V", false);
                                            visitInsn(ARETURN);
                                        }
                                    };
                                }

                                return original;
                            }
                        };
                    }
                }));

        producer.produce(new BytecodeTransformerBuildItem(SmallRyeConfigProviderResolver.class.getName(),
                new BiFunction<>() {
                    @Override
                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                    String signature, String[] exceptions) {
                                MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("getFactoryFor")) {
                                    return new MethodVisitor(Gizmo.ASM_API_VERSION, original) {
                                        @Override
                                        public void visitCode() {
                                            super.visitCode();
                                            String binaryName = QuarkusConfigFactory.class.getName().replace('.',
                                                    '/');
                                            visitTypeInsn(NEW, binaryName);
                                            visitInsn(DUP);
                                            visitMethodInsn(INVOKESPECIAL,
                                                    binaryName, CTOR_NAME, "()V", false);
                                            visitInsn(ARETURN);
                                        }
                                    };
                                }

                                return original;
                            }
                        };
                    }
                }));
    }

    private Services determineValidatorImplementation(List<ServiceProviderBuildItem> services) {
        String validatorImpl = null;
        List<String> interceptorImpls = new ArrayList<>();
        List<String> interceptorFactoryImpls = new ArrayList<>();
        List<String> converters = new ArrayList<>();
        for (ServiceProviderBuildItem service : services) {
            String serviceInterface = service.getServiceInterface();
            if (CONFIG_VALIDATOR_CLASS_NAME.equals(serviceInterface)) {
                if (service.providers().size() == 1) {
                    validatorImpl = service.providers().get(0);
                }
            } else if (CONFIG_SOURCE_INTERCEPTOR_CLASS_NAME.equals(serviceInterface)) {
                interceptorImpls.addAll(service.providers());
            } else if (CONFIG_SOURCE_INTERCEPTOR_FACTORY_CLASS_NAME.equals(serviceInterface)) {
                interceptorFactoryImpls.addAll(service.providers());
            } else if (CONVERTER_CLASS_NAME.equals(serviceInterface)) {
                converters.addAll(service.providers());
            }
        }
        return new Services(validatorImpl, interceptorImpls, interceptorFactoryImpls, converters);
    }

    private static class Services {
        final String validator;
        final List<String> interceptorImpls;
        final List<String> interceptorFactoryImpls;
        final List<String> converters;

        private Services(String validator, List<String> interceptorImpls,
                List<String> interceptorFactoryImpls, List<String> converters) {
            this.validator = validator;
            this.interceptorImpls = interceptorImpls;
            this.interceptorFactoryImpls = interceptorFactoryImpls;
            this.converters = converters;
        }
    }
}
