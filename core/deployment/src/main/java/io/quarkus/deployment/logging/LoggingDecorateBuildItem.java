package io.quarkus.deployment.logging;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains information to decorate the Log output. Can be used by extensions that output the log / stacktraces,
 * for example the error page.
 *
 * Also see io.quarkus.runtime.logging.DecorateStackUtil to assist with the decoration
 */
public final class LoggingDecorateBuildItem extends SimpleBuildItem {
    private final Path srcMainJava;
    private final CompositeIndex knowClassesIndex;

    public LoggingDecorateBuildItem(Path srcMainJava, CompositeIndex knowClassesIndex) {
        this.srcMainJava = srcMainJava;
        this.knowClassesIndex = knowClassesIndex;
    }

    public Path getSrcMainJava() {
        return srcMainJava;
    }

    public CompositeIndex getKnowClassesIndex() {
        return knowClassesIndex;
    }

    /**
     * Returns a list of ordered known classes. Normally, we don't require the build items
     * to have reproducible collections, since stabilizing order is a responsibility of
     * {@code io.quarkus.deployment.annotations.Record} annotated build steps.
     * <p>
     * However, this build item is used by the number of bytecode-recording build steps.
     * Repeating the ordering logic in all of them seems like a waste, so we do it here instead.
     *
     * @return a list of ordered known classes, sorted by their name.
     *
     */
    public List<String> getKnowClasses() {
        List<String> knowClasses = new ArrayList<>();
        Collection<ClassInfo> knownClasses = knowClassesIndex.getKnownClasses();
        for (ClassInfo ci : knownClasses) {
            knowClasses.add(ci.name().toString());
        }
        Collections.sort(knowClasses);
        return knowClasses;
    }

}
