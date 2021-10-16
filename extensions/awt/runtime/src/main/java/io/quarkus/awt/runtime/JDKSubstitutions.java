package io.quarkus.awt.runtime;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.PropertyResourceBundle;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK11OrLater;

/**
 * TODO: This is just a workaround,
 * see AwtProcessor#yankI18NPropertiesFromHostJDK
 */
@TargetClass(className = "com.sun.imageio.plugins.common.I18NImpl", onlyWith = JDK11OrLater.class)
final class Target_com_sun_imageio_plugins_common_I18NImpl {

    @Substitute
    private static String getString(String className, String resource_name, String key) {
        // The property file is now stored in the root of the tree
        resource_name = "/" + resource_name;
        PropertyResourceBundle bundle = null;
        try {
            // className ignored, there is only one such file in the imageio anyway
            InputStream stream = MethodHandles.lookup().lookupClass().getResourceAsStream(resource_name);
            bundle = new PropertyResourceBundle(stream);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return (String) bundle.handleGetObject(key);
    }
}
