package io.quarkus.hibernate.validator.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.nativeimage.hosted.Feature;

/**
 * Disables logging during the analysis phase
 */
public class DisableLoggingFeature implements Feature {

    private static final String[] CATEGORIES = {
            "org.hibernate.validator.internal.util.Version",
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
            Logger logger = Logger.getLogger(category);
            logger.setLevel(level);
        }
    }

    @Override
    public String getDescription() {
        return "Disables INFO logging during the analysis phase for the " + Arrays.toString(CATEGORIES) + " categories";
    }
}
