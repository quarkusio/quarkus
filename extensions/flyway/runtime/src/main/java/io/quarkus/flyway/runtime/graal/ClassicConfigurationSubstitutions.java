package io.quarkus.flyway.runtime.graal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.extensibility.ConfigurationExtension;
import org.flywaydb.core.internal.plugin.PluginRegister;
import org.flywaydb.core.internal.util.MergeUtils;

import com.google.gson.Gson;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.flywaydb.core.api.configuration.ClassicConfiguration")
public final class ClassicConfigurationSubstitutions {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Pattern ANY_WORD_BETWEEN_TWO_QUOTES_PATTERN = Pattern.compile("\"([^\"]*)\"");

    @Alias
    PluginRegister pluginRegister;

    @Substitute
    private void determineKeysToRemoveAndRemoveFromProps(HashMap<String, Map<String, Object>> configExtensionsPropertyMap,
            List<String> keysToRemove, Map<String, String> props) {
        for (Map.Entry<String, Map<String, Object>> property : configExtensionsPropertyMap.entrySet()) {
            ConfigurationExtension cfg = null;
            for (ConfigurationExtension c : pluginRegister.getPlugins(ConfigurationExtension.class)) {
                if (c.getClass().toString().equals(property.getKey())) {
                    cfg = c;
                    break;
                }
            }
            if (cfg != null) {
                Map<String, Object> mp = property.getValue();
                try {
                    Gson gson = new Gson();
                    ConfigurationExtension newConfigurationExtension = gson.fromJson(gson.toJson(mp), cfg.getClass());
                    MergeUtils.mergeModel(newConfigurationExtension, cfg);
                } catch (Exception e) {
                    Matcher matcher = ANY_WORD_BETWEEN_TWO_QUOTES_PATTERN.matcher(e.getMessage());
                    if (matcher.find()) {
                        String errorProperty = matcher.group(1);
                        List<String> propsToRemove = new ArrayList<>();
                        for (String k : keysToRemove) {
                            if (k.endsWith(errorProperty)) {
                                propsToRemove.add(k);
                            }
                        }
                        keysToRemove.removeAll(propsToRemove);
                    }
                }
            }
        }

        props.keySet().removeAll(keysToRemove);
    }

}
