package io.quarkus.hibernate.orm.runtime.graal;

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
            "org.hibernate.Version",
            "org.hibernate.annotations.common.Version",
            "SQL dialect",
            "org.hibernate.cfg.Environment",
            "org.hibernate.orm.connections.pooling"
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
        return "Disables INFO logging during the analysis phase";
    }
}
