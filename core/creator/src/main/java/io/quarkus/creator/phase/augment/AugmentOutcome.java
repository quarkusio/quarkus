package io.quarkus.creator.phase.augment;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Represents an outcome of {@link AugmentPhase}
 *
 * @author Alexey Loubyansky
 */
public interface AugmentOutcome {

    /**
     * Directory containing original user application classes.
     *
     * @return directory containing original user application classes
     */
    Path getAppClassesDir();

    /**
     * Directory containing bytecode-transformed user application classes.
     * Depending on the application, this directory may be empty.
     *
     * @return directory containing transformed user application classes
     */
    Path getTransformedClassesDir();

    /**
     * Directory containing classes generated during augmentation.
     *
     * @return directory containing generated classes
     */
    Path getWiringClassesDir();

    /**
     * Directory containing config files used by the application
     *
     * @return directory containing config files
     */
    Path getConfigDir();

    /**
     * This returns a map of jar files to classes from that jar that have been transformed. These classes should
     * not be copied into the final output, as they are present in the transformed classes set, so will need to
     * be removed from the jar file.
     *
     * Note that the classes are in file name format (i.e. with / instead of . and with the .class suffix)
     *
     * @return the transformed class files
     */
    Map<Path, Set<String>> getTransformedClassesByJar();

}
