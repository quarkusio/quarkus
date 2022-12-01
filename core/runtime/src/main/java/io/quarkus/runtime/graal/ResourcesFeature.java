package io.quarkus.runtime.graal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.graalvm.nativeimage.hosted.Feature;

import io.quarkus.runtime.ResourceHelper;

public class ResourcesFeature implements Feature {

    public static final String META_INF_QUARKUS_NATIVE_RESOURCES_TXT = "META-INF/quarkus-native-resources.txt";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(META_INF_QUARKUS_NATIVE_RESOURCES_TXT);
        if (resourceAsStream != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(resourceAsStream))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (!line.isEmpty()) {
                        ResourceHelper.registerResources(line);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Register each line in " + META_INF_QUARKUS_NATIVE_RESOURCES_TXT + " as a resource on Substrate VM";
    }
}
