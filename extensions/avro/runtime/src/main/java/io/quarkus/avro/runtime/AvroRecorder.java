package io.quarkus.avro.runtime;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.ClassSecurityValidator;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AvroRecorder {

    private static final Logger log = Logger.getLogger(AvroRecorder.class);

    /**
     * Installs a global Avro {@link ClassSecurityValidator} that trusts the given classes and packages.
     * <p>
     * The trusted classes are collected at build time from every {@code io.quarkus.avro.spi.AvroTrustedClassBuildItem}
     * (e.g. Avro-generated records and Protobuf-generated messages) plus {@code quarkus.avro.trusted-classes}; the
     * trusted packages come from {@code quarkus.avro.trusted-packages}.
     * <p>
     * The custom predicate is composed with the validator that is already installed (Avro's
     * {@link ClassSecurityValidator#DEFAULT} by default, or one set by another library before this runs) rather than
     * replacing it, so previously trusted classes and the
     * {@code org.apache.avro.SERIALIZABLE_CLASSES}/{@code org.apache.avro.SERIALIZABLE_PACKAGES} system properties keep
     * working as an additional escape hatch.
     * <p>
     * A shutdown task restores the default validator to prevent continuous nesting of validators during reloads in dev mode.
     *
     * @param shutdownContext used to register the validator reset on shutdown
     * @param trustedClasses fully qualified names of the trusted classes
     * @param trustedPackages package names whose classes are trusted
     */
    public void setupClassSecurityValidator(ShutdownContext shutdownContext, Set<String> trustedClasses,
            List<String> trustedPackages) {
        QuarkusAvroClassSecurityPredicate predicate = new QuarkusAvroClassSecurityPredicate(trustedClasses,
                trustedPackages);
        ClassSecurityValidator.setGlobal(ClassSecurityValidator.composite(predicate, ClassSecurityValidator.getGlobal()));
        shutdownContext.addShutdownTask(() -> ClassSecurityValidator.setGlobal(ClassSecurityValidator.DEFAULT));
    }

    public void clearStaticCaches() {
        try {
            Field instanceField = SpecificData.class.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            SpecificData data = (SpecificData) instanceField.get(null);
            Field classCache = SpecificData.class.getDeclaredField("classCache");
            classCache.setAccessible(true);
            Map<String, Class> classCacheMap = (Map<String, Class>) classCache.get(data);
            classCacheMap.clear();
        } catch (Throwable t) {
            log.error("Failed to clear Avro cache", t);
        }
    }
}
