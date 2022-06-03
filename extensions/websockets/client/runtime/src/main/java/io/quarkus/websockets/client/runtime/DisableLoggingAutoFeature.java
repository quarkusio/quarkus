package io.quarkus.websockets.client.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.nativeimage.hosted.Feature;

import com.oracle.svm.core.annotate.AutomaticFeature;

/**
 * Disables logging during the analysis phase
 */
@AutomaticFeature
public class DisableLoggingAutoFeature implements Feature {

    private static final String[] CATEGORIES = {
            "io.undertow.websockets",
    };

    private final Map<String, Level> categoryMap = new HashMap<>(CATEGORIES.length);

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (String category : CATEGORIES) {
            Logger logger = Logger.getLogger(category);
            categoryMap.put(category, logger.getLevel());
            logger.setLevel(Level.WARNING);
        }
    }

    @Override
    public void afterAnalysis(AfterAnalysisAccess access) {
        for (String category : CATEGORIES) {
            Level level = categoryMap.remove(category);
            if (level != null) {
                Logger logger = Logger.getLogger(category);
                logger.setLevel(level);
            }
        }
    }
}
