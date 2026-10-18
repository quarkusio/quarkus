package io.quarkus.runtime.graal;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.nativeimage.hosted.Feature;

/**
 * Disables logging during the analysis phase
 */
public class DisableLoggingFeature implements Feature {

    /**
     * Category to configure to WARNING
     */
    private static final String[] WARN_CATEGORIES = {
            "org.jboss.threads" };

    /**
     * Category to configure to ERROR
     */
    private static final String[] ERROR_CATEGORIES = {
            "io.netty.resolver.dns.DnsServerAddressStreamProviders"
    };

    private final Map<String, Level> categoryMap = new HashMap<>(WARN_CATEGORIES.length + ERROR_CATEGORIES.length);

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (String category : WARN_CATEGORIES) {
            Logger logger = Logger.getLogger(category);
            categoryMap.put(category, logger.getLevel());
            logger.setLevel(Level.WARNING);
        }

        for (String category : ERROR_CATEGORIES) {
            Logger logger = Logger.getLogger(category);
            categoryMap.put(category, logger.getLevel());
            logger.setLevel(Level.SEVERE);
        }
    }

    @Override
    public void afterAnalysis(AfterAnalysisAccess access) {
        for (Map.Entry<String, Level> entry : categoryMap.entrySet()) {
            Logger logger = Logger.getLogger(entry.getKey());
            logger.setLevel(entry.getValue());
        }
        categoryMap.clear();
    }

    @Override
    public String getDescription() {
        return "Adapts logging during the analysis phase";
    }
}
