package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.smallrye.classfile.ClassFile;

class JarTreeShakerTest {

    private static final ClassDesc CD_Bootstraps = ClassDesc.of("io.quarkus.core.Bootstraps");
    private static final ClassDesc CD_Bootstrap = ClassDesc.of("com.example.Bootstrap");
    private static final ClassDesc CD_Wrapper = ClassDesc.of("com.example.Wrapper");

    private static final DirectMethodHandleDesc LOAD_CLASS = ConstantDescs.ofConstantBootstrap(
            CD_Bootstraps, "loadClass", ConstantDescs.CD_Class, ConstantDescs.CD_String);

    @Test
    void constantDynamicReferencesAreCollected() {
        DynamicConstantDesc<?> condy = DynamicConstantDesc.ofNamed(LOAD_CLASS, "captured", ConstantDescs.CD_Class,
                "Lcom/example/Captured;");
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(condy), null);

        assertThat(refs).contains("io.quarkus.core.Bootstraps", "com.example.Captured");
    }

    @Test
    void nestedConstantDynamicReferencesAreCollected() {
        DynamicConstantDesc<?> inner = DynamicConstantDesc.ofNamed(LOAD_CLASS, "inner", ConstantDescs.CD_Class,
                "[Lcom/example/Element;");
        DirectMethodHandleDesc outerBootstrap = ConstantDescs.ofConstantBootstrap(CD_Bootstrap, "wrap", CD_Wrapper,
                ConstantDescs.CD_Class);
        DynamicConstantDesc<?> outer = DynamicConstantDesc.ofNamed(outerBootstrap, "outer", CD_Wrapper, inner);
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(outer), null);

        assertThat(refs).contains("com.example.Bootstrap", "com.example.Wrapper", "io.quarkus.core.Bootstraps",
                "com.example.Element");
    }

    @Test
    void plainStringConstantsAreNotTreatedAsDescriptors() {
        DirectMethodHandleDesc bootstrap = ConstantDescs.ofConstantBootstrap(CD_Bootstrap, "name",
                ConstantDescs.CD_String, ConstantDescs.CD_String);
        DynamicConstantDesc<?> condy = DynamicConstantDesc.ofNamed(bootstrap, "name", ConstantDescs.CD_String,
                "Literal;");
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(condy), null);

        assertThat(refs).contains("com.example.Bootstrap").doesNotContain("iteral", "Literal");
    }

    @Test
    void descriptorArgumentsOfOtherBootstrapsAreNotFollowed() {
        DirectMethodHandleDesc bootstrap = ConstantDescs.ofConstantBootstrap(CD_Bootstrap, "name",
                ConstantDescs.CD_String, ConstantDescs.CD_String);
        DynamicConstantDesc<?> condy = DynamicConstantDesc.ofNamed(bootstrap, "name", ConstantDescs.CD_String,
                "Lcom/example/NotLoaded;");
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(condy), null);

        assertThat(refs).contains("com.example.Bootstrap").doesNotContain("com.example.NotLoaded");
    }

    @Test
    void ldcMethodHandleReferencesAreCollected() {
        MethodHandleDesc handle = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC,
                ClassDesc.of("com.example.HandleOwner"), "target",
                MethodTypeDesc.of(ConstantDescs.CD_void, ClassDesc.of("com.example.HandleArg")));
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(handle), null);

        assertThat(refs).contains("com.example.HandleOwner", "com.example.HandleArg");
    }

    @Test
    void ldcMethodTypeReferencesAreCollected() {
        MethodTypeDesc methodType = MethodTypeDesc.of(ConstantDescs.CD_void, ClassDesc.of("com.example.MethodTypeArg"));
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithLdc(methodType), null);

        assertThat(refs).contains("com.example.MethodTypeArg");
    }

    @Test
    void lambdaInstantiatedMethodTypeReferencesAreCollected() {
        // what javac emits for Function<Instantiated, Instantiated> f = Util::identity with <T> T identity(T t):
        // Instantiated only appears in the instantiatedMethodType bootstrap argument
        ClassDesc instantiated = ClassDesc.of("com.example.Instantiated");
        MethodTypeDesc erased = MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object);
        DirectMethodHandleDesc metafactory = ConstantDescs.ofCallsiteBootstrap(
                ClassDesc.of("java.lang.invoke.LambdaMetafactory"), "metafactory", ConstantDescs.CD_CallSite,
                ConstantDescs.CD_MethodType, ConstantDescs.CD_MethodHandle, ConstantDescs.CD_MethodType);
        DynamicCallSiteDesc callSite = DynamicCallSiteDesc.of(metafactory, "apply",
                MethodTypeDesc.of(ClassDesc.of("java.util.function.Function")),
                erased,
                MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, ClassDesc.of("com.example.Util"),
                        "identity", erased),
                MethodTypeDesc.of(instantiated, instantiated));
        Set<String> refs = JarTreeShaker.extractReferencesFromBytecode(classWithInvokeDynamic(callSite), null);

        assertThat(refs).contains("com.example.Util", "com.example.Instantiated");
    }

    private static byte[] classWithInvokeDynamic(DynamicCallSiteDesc callSite) {
        return ClassFile.of().build(ClassDesc.of("com.example.Generated"), classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC);
            classBuilder.withMethodBody("get", MethodTypeDesc.of(ConstantDescs.CD_Object),
                    ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> {
                        code.invokedynamic(callSite);
                        code.areturn();
                    });
        });
    }

    private static byte[] classWithLdc(ConstantDesc constant) {
        return ClassFile.of().build(ClassDesc.of("com.example.Generated"), classBuilder -> {
            classBuilder.withFlags(ClassFile.ACC_PUBLIC);
            classBuilder.withMethodBody("get", MethodTypeDesc.of(ConstantDescs.CD_Object),
                    ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> {
                        code.loadConstant(constant);
                        code.areturn();
                    });
        });
    }
}
