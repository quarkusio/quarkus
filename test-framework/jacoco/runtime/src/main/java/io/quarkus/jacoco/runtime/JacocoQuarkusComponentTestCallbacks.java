package io.quarkus.jacoco.runtime;

import java.io.IOException;
import java.util.Collection;
import java.util.function.BiFunction;

import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.processor.BytecodeTransformer;
import io.quarkus.test.component.QuarkusComponentTestCallbacks;
import io.quarkus.test.component.QuarkusComponentTestClassLoader;

public class JacocoQuarkusComponentTestCallbacks implements QuarkusComponentTestCallbacks {

    private static final Logger LOG = Logger.getLogger(JacocoQuarkusComponentTestCallbacks.class);

    @Override
    public void beforeBuild(BeforeBuildContext beforeBuild) {
        // Instrument all classes loaded by QuarkusComponentTestClassLoader
        Collection<ClassInfo> toTransform = beforeBuild.getImmutableBeanArchiveIndex()
                .getKnownClasses()
                .stream()
                .filter(c -> !QuarkusComponentTestClassLoader.mustDelegateToParent(c.name().toString()))
                .toList();
        Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
        for (ClassInfo clazz : toTransform) {
            beforeBuild.addBytecodeTransformer(
                    BytecodeTransformer.forInputTransformer(clazz.name().toString(), new BiFunction<String, byte[], byte[]>() {
                        @Override
                        public byte[] apply(String className, byte[] bytes) {
                            try {
                                byte[] enhanced = instrumenter.instrument(bytes, className);
                                if (enhanced == null) {
                                    return bytes;
                                }
                                return enhanced;
                            } catch (IOException e) {
                                LOG.warnf(e,
                                        "Unable to instrument class %s with JaCoCo, keeping the original class",
                                        className);
                                return bytes;
                            }
                        }
                    }));
        }
    }
}
