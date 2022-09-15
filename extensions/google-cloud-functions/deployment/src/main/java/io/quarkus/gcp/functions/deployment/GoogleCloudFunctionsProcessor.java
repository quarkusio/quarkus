package io.quarkus.gcp.functions.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Named;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.CloudEventsFunction;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.RawBackgroundFunction;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.gcp.functions.GoogleCloudFunctionInfo;
import io.quarkus.gcp.functions.GoogleCloudFunctionRecorder;
import io.quarkus.gcp.functions.GoogleCloudFunctionsConfig;

public class GoogleCloudFunctionsProcessor {
    private static final String FEATURE_NAME = "google-cloud-functions";
    public static final DotName DOTNAME_NAMED = DotName.createSimple(Named.class.getName());
    public static final DotName DOTNAME_HTTP_FUNCTION = DotName.createSimple(HttpFunction.class.getName());
    public static final DotName DOTNAME_BACKGROUND_FUNCTION = DotName.createSimple(BackgroundFunction.class.getName());
    public static final DotName DOTNAME_RAW_BACKGROUND_FUNCTION = DotName.createSimple(RawBackgroundFunction.class.getName());
    public static final DotName DOTNAME_CLOUD_EVENT_FUNCTION = DotName.createSimple(CloudEventsFunction.class.getName());

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE_NAME);
    }

    @BuildStep
    public RunTimeConfigurationDefaultBuildItem disableBanner() {
        // the banner is not displayed well inside the Google Cloud Function logs
        return new RunTimeConfigurationDefaultBuildItem("quarkus.banner.enabled", "false");
    }

    @BuildStep
    public List<CloudFunctionBuildItem> discoverFunctionClass(CombinedIndexBuildItem combinedIndex,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans)
            throws BuildException {
        IndexView index = combinedIndex.getIndex();
        Collection<ClassInfo> httpFunctions = index.getAllKnownImplementors(DOTNAME_HTTP_FUNCTION);
        Collection<ClassInfo> backgroundFunctions = index.getAllKnownImplementors(DOTNAME_BACKGROUND_FUNCTION);
        Collection<ClassInfo> rawBackgroundFunctions = index.getAllKnownImplementors(DOTNAME_RAW_BACKGROUND_FUNCTION);
        Collection<ClassInfo> cloudEventFunctions = index.getAllKnownImplementors(DOTNAME_CLOUD_EVENT_FUNCTION);

        List<CloudFunctionBuildItem> cloudFunctions = new ArrayList<>();
        cloudFunctions.addAll(
                registerFunctions(unremovableBeans, httpFunctions, GoogleCloudFunctionInfo.FunctionType.HTTP));
        cloudFunctions.addAll(
                registerFunctions(unremovableBeans, backgroundFunctions, GoogleCloudFunctionInfo.FunctionType.BACKGROUND));
        cloudFunctions.addAll(
                registerFunctions(unremovableBeans, rawBackgroundFunctions,
                        GoogleCloudFunctionInfo.FunctionType.RAW_BACKGROUND));
        cloudFunctions.addAll(
                registerFunctions(unremovableBeans, cloudEventFunctions, GoogleCloudFunctionInfo.FunctionType.CLOUD_EVENT));

        if (cloudFunctions.isEmpty()) {
            throw new BuildException("No Google Cloud Function found on the classpath", Collections.emptyList());
        }
        return cloudFunctions;
    }

    private List<CloudFunctionBuildItem> registerFunctions(BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            Collection<ClassInfo> functions,
            GoogleCloudFunctionInfo.FunctionType functionType) {
        List<CloudFunctionBuildItem> buildItems = new ArrayList<>();
        for (ClassInfo classInfo : functions) {
            String className = classInfo.name().toString();
            unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(className));
            List<AnnotationInstance> annotationInstances = classInfo.annotationsMap().get(DOTNAME_NAMED);
            CloudFunctionBuildItem buildItem = new CloudFunctionBuildItem(className, functionType);
            if (annotationInstances != null) {
                buildItem.setBeanName(annotationInstances.get(0).value().asString());
            }
            buildItems.add(buildItem);
        }
        return buildItems;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void selectDelegate(List<CloudFunctionBuildItem> cloudFunctions,
            GoogleCloudFunctionsConfig config,
            GoogleCloudFunctionRecorder recorder) throws BuildException {

        List<GoogleCloudFunctionInfo> functionInfos = cloudFunctions.stream()
                .map(CloudFunctionBuildItem::build)
                .collect(Collectors.toList());

        recorder.selectDelegate(config, functionInfos);
    }
}
