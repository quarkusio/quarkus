package io.quarkus.quartz.deployment;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.ByteDataImpl;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.FileUtils;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

/**
 * Quartz is compiled using references to classes in the javax packages;
 * we need to transform these to fix compatibility with jakarta packages.
 * We do this by leveraging the Eclipse Transformer project during Augmentation, so
 * that end users don't need to bother.
 */
public class JakartaEnablement {

    private static final List<String> CLASSES_NEEDING_TRANSFORMATION = List.of(
            "org.quartz.ExecuteInJTATransaction",
            "org.quartz.ee.servlet.QuartzInitializerServlet",
            "org.quartz.ee.servlet.QuartzInitializerListener",
            "org.quartz.ee.jta.JTAJobRunShell",
            "org.quartz.ee.jta.UserTransactionHelper",
            "org.quartz.ee.jta.UserTransactionHelper$UserTransactionWithContext",
            "org.quartz.xml.XMLSchedulingDataProcessor",
            "org.quartz.impl.jdbcjobstore.JTANonClusteredSemaphore");

    @BuildStep
    void transformToJakarta(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (QuarkusClassLoader.isClassPresentAtRuntime("jakarta.transaction.Transaction")) {
            JakartaTransformer tr = new JakartaTransformer();
            for (String classname : CLASSES_NEEDING_TRANSFORMATION) {
                final BytecodeTransformerBuildItem item = new BytecodeTransformerBuildItem.Builder()
                        .setCacheable(true)
                        .setContinueOnFailure(false)
                        .setClassToTransform(classname)
                        .setClassReaderOptions(ClassReader.SKIP_DEBUG)
                        .setInputTransformer(tr::transform)
                        .build();
                transformers.produce(item);
            }
        }
    }

    private static class JakartaTransformer {

        private final Logger logger;
        private final ActionContext ctx;
        // We need to prevent the Eclipse Transformer to adjust the "javax" packages.
        // Thus why we split the strings.
        private static final Map<String, String> renames = Map.of("javax" + ".transaction", "jakarta.transaction",
                "javax" + ".servlet", "jakarta.servlet",
                "javax" + ".xml.bind", "jakarta.xml.bind");

        JakartaTransformer() {
            logger = LoggerFactory.getLogger("JakartaTransformer");
            //N.B. we enable only this single transformation of package renames, not the full set of capabilities of Eclipse Transformer;
            //this might need tailoring if the same idea gets applied to a different context.
            ctx = new ActionContextImpl(logger,
                    new SelectionRuleImpl(logger, Collections.emptyMap(), Collections.emptyMap()),
                    new SignatureRuleImpl(logger, renames, null, null, null, null, null, Collections.emptyMap()));
        }

        byte[] transform(final String name, final byte[] bytes) {
            logger.debug("Jakarta EE compatibility enhancer for Quarkus: transforming " + name);
            final ClassActionImpl classTransformer = new ClassActionImpl(ctx);
            final ByteBuffer input = ByteBuffer.wrap(bytes);
            final ByteData inputData = new ByteDataImpl(name, input, FileUtils.DEFAULT_CHARSET);
            final ByteData outputData = classTransformer.apply(inputData);
            return outputData.buffer().array();
        }
    }

}
