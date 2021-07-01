package io.quarkus.runtime.graal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.graalvm.nativeimage.hosted.Feature;

import com.oracle.svm.core.annotate.AutomaticFeature;

import io.quarkus.runtime.ResourceHelper;

@AutomaticFeature
public class ResourcesFeature implements Feature {

    public static final String META_INF_QUARKUS_NATIVE_RESOURCES_TXT = "META-INF/quarkus-native-resources.txt";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(META_INF_QUARKUS_NATIVE_RESOURCES_TXT)))) {
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
