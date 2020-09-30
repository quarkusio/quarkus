package io.quarkus.freemarker.runtime;

import java.util.List;
import java.util.Map;

import freemarker.template.TemplateModel;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "freemarker", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class FreemarkerBuildConfig {

    /**
     * Comma-separated list of absolute resource paths to scan recursively for templates.
     * All tree folder from 'resource-paths' will be added as a resource.
     * Unprefixed locations or locations starting with classpath will be processed in the same way.
     */
    @ConfigItem(defaultValue = "freemarker/templates")
    public List<String> resourcePaths;

    /**
     * List of directives to register with format name=classname
     * 
     * @see freemarker.template.Configuration#setSharedVariable(String, TemplateModel)
     */
    @ConfigItem
    public Map<String, String> directive;

}
