package org.jboss.shamrock.deployment.steps;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.ConfigurationCustomConverterBuildItem;
import org.jboss.shamrock.runtime.configuration.FileConverter;

/**
 * Converters for built-in type support.
 */
public class BuiltInConvertersBuildStep {

    @BuildStep
    public List<ConfigurationCustomConverterBuildItem> execute() {
        return Collections.singletonList(
            new ConfigurationCustomConverterBuildItem(100, File.class, FileConverter.class)
        );
    }
}
