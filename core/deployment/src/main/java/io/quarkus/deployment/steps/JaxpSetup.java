package io.quarkus.deployment.steps;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;

/**
 * A setup class which creates substitutions for JAXP factories that do not use service loader to find their instances.
 * This allows unused JAXP classes to be dead-code eliminated when compiling to a native image.
 */
public final class JaxpSetup {

    @BuildStep
    void generateSubstitutions(
            Consumer<GeneratedClassBuildItem> classConsumer) throws IOException {
        // todo: application class loader or resources
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final ClassOutput classOutput = new ClassOutput() {
            public void write(final String name, final byte[] data) {
                classConsumer.accept(new GeneratedClassBuildItem(false, name, data));
            }
        };

        // javax.xml.datatype.DatatypeFactory
        generateServiceClass(
                classOutput,
                cl,
                DatatypeConfigurationException.class,
                null,
                MethodDescriptor.ofMethod(DatatypeFactory.class, "newInstance", DatatypeFactory.class),
                MethodDescriptor.ofMethod(DatatypeFactory.class, "newInstance", DatatypeFactory.class, String.class,
                        ClassLoader.class));

        // javax.xml.parsers.SAXParserFactory
        generateServiceClass(
                classOutput,
                cl,
                IllegalArgumentException.class,
                null,
                MethodDescriptor.ofMethod(SAXParserFactory.class, "newInstance", SAXParserFactory.class),
                MethodDescriptor.ofMethod(SAXParserFactory.class, "newInstance", SAXParserFactory.class, String.class,
                        ClassLoader.class));

        // javax.xml.validation.SchemaFactory
        generateServiceClass(
                classOutput,
                cl,
                IllegalArgumentException.class,
                (mc, inst) -> mc.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(SchemaFactory.class, "isSchemaLanguageSupported", boolean.class,
                                String.class),
                        inst,
                        mc.getMethodParam(0)),
                MethodDescriptor.ofMethod(SchemaFactory.class, "newInstance", SchemaFactory.class, String.class),
                MethodDescriptor.ofMethod(SchemaFactory.class, "newInstance", SchemaFactory.class, String.class, String.class,
                        ClassLoader.class));

        // javax.xml.transform.TransformerFactory
        generateServiceClass(
                classOutput,
                cl,
                IllegalArgumentException.class,
                null,
                MethodDescriptor.ofMethod(TransformerFactory.class, "newInstance", TransformerFactory.class),
                MethodDescriptor.ofMethod(TransformerFactory.class, "newInstance", TransformerFactory.class, String.class,
                        ClassLoader.class));

        // javax.xml.stream.XMLEventFactory
        generateServiceClass(
                classOutput,
                cl,
                IllegalArgumentException.class,
                null,
                MethodDescriptor.ofMethod(XMLEventFactory.class, "newFactory", XMLEventFactory.class),
                MethodDescriptor.ofMethod(XMLEventFactory.class, "newFactory", XMLEventFactory.class, String.class,
                        ClassLoader.class),
                MethodDescriptor.ofMethod(XMLEventFactory.class, "newInstance", XMLEventFactory.class),
                // this one is deprecated but still part of the API
                MethodDescriptor.ofMethod(XMLEventFactory.class, "newInstance", XMLEventFactory.class, String.class,
                        ClassLoader.class));

        // javax.xml.stream.XMLInputFactory
        generateServiceClass(
                classOutput,
                cl,
                IllegalArgumentException.class,
                null,
                MethodDescriptor.ofMethod(XMLInputFactory.class, "newFactory", XMLInputFactory.class),
                MethodDescriptor.ofMethod(XMLInputFactory.class, "newFactory", XMLInputFactory.class, String.class,
                        ClassLoader.class),
                MethodDescriptor.ofMethod(XMLInputFactory.class, "newInstance", XMLInputFactory.class),
                // this one is deprecated but still part of the API
                MethodDescriptor.ofMethod(XMLInputFactory.class, "newInstance", XMLInputFactory.class, String.class,
                        ClassLoader.class));

        // javax.xml.stream.XMLOutputFactory
        generateServiceClass(
                classOutput,
                cl,
                IllegalArgumentException.class,
                null,
                MethodDescriptor.ofMethod(XMLOutputFactory.class, "newFactory", XMLOutputFactory.class),
                MethodDescriptor.ofMethod(XMLOutputFactory.class, "newFactory", XMLOutputFactory.class, String.class,
                        ClassLoader.class),
                MethodDescriptor.ofMethod(XMLOutputFactory.class, "newInstance", XMLOutputFactory.class),
                // this one is deprecated but still part of the API
                MethodDescriptor.ofMethod(XMLOutputFactory.class, "newInstance", XMLOutputFactory.class, String.class,
                        ClassLoader.class));

        // org.xml.sax.helpers.XMLReaderFactory
        generateServiceClass(
                classOutput,
                cl,
                SAXException.class,
                null,
                MethodDescriptor.ofMethod(XMLReaderFactory.class, "createXMLReader", XMLReader.class));

        // javax.xml.xpath.XPathFactory
        generateServiceClass(
                classOutput,
                cl,
                XPathFactoryConfigurationException.class,
                (mc, inst) -> mc.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(XPathFactory.class, "isObjectModelSupported", boolean.class, String.class),
                        inst,
                        mc.getMethodParam(0)),
                MethodDescriptor.ofMethod(XPathFactory.class, "newInstance", XPathFactory.class, String.class),
                MethodDescriptor.ofMethod(XPathFactory.class, "newInstance", XPathFactory.class, String.class, String.class,
                        ClassLoader.class));
    }

    private static void generateServiceClass(ClassOutput classOutput, ClassLoader cl,
            Class<? extends Exception> exceptionType,
            BiFunction<BytecodeCreator, ResultHandle, ResultHandle> checker,
            MethodDescriptor factoryMethod, MethodDescriptor... additionalFactories) throws IOException {
        final String declaringClass = factoryMethod.getDeclaringClass();
        // usually but not always the same as the declaring class
        final String factoryType = factoryMethod.getReturnType();

        // Determine ctor parameter types
        final String[] parameterTypes = factoryMethod.getParameterTypes();
        final int paramCnt = parameterTypes.length;

        try (ClassCreator cc = ClassCreator.builder()
                .className("io.quarkus.runtime.generated.graal.Target_" + declaringClass.replace('.', '_').replace('/', '_'))
                .classOutput(classOutput)
                .setFinal(true)
                .build()) {

            cc.addAnnotation("com.oracle.svm.core.annotate.TargetClass")
                    .addValue("value", Type.getObjectType(declaringClass.replace('.', '/')));

            // primary factory
            try (MethodCreator mc = cc.getMethodCreator(factoryMethod)) {
                mc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                mc.addAnnotation("com.oracle.svm.core.annotate.Substitute");
                mc.addException(exceptionType);

                final Set<String> factories = ServiceUtil.classNamesNamedIn(cl, factoryType);
                final Iterator<String> iterator = factories.iterator();
                if (iterator.hasNext()) {
                    ResultHandle[] args = new ResultHandle[paramCnt];
                    for (int i = 0; i < paramCnt; i++) {
                        args[i] = mc.getMethodParam(i);
                    }
                    do {
                        final String factory = iterator.next();
                        try (TryBlock tb = mc.tryBlock()) {
                            final ResultHandle instance = tb.newInstance(MethodDescriptor.ofConstructor(factory), args);
                            if (checker != null) {
                                tb.ifNonZero(checker.apply(mc, instance)).falseBranch().breakScope(tb);
                            }
                            tb.returnValue(instance);
                            try (CatchBlockCreator cbc = tb.addCatch(NoClassDefFoundError.class)) {
                                cbc.breakScope(tb);
                            }
                        }
                    } while (iterator.hasNext());
                }
                mc.throwException(exceptionType, "No suitable instance found");
            }

            // secondary factories
            if (additionalFactories != null) {
                for (MethodDescriptor desc : additionalFactories) {
                    try (MethodCreator mc = cc.getMethodCreator(desc)) {
                        mc.setModifiers(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                        mc.addAnnotation("com.oracle.svm.core.annotate.Substitute");

                        ResultHandle[] args = new ResultHandle[paramCnt];
                        // copy the first N arguments to the delegate
                        for (int i = 0; i < paramCnt; i++) {
                            args[i] = mc.getMethodParam(i);
                        }
                        mc.returnValue(mc.invokeStaticMethod(factoryMethod, args));
                    }
                }
            }
        }
    }
}
