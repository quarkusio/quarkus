package io.quarkus.deployment.builditem.substrate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.NativeEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;

/**
 * @deprecated This class is temporarily used to convert classes from the {@link io.quarkus.deployment.builditem.substrate}
 *             package into classes from the {@link io.quarkus.deployment.builditem.nativeimage} package.
 */
@Deprecated
public class DeprecatedBuildItemProcessor {

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem> reflectiveClasses(
            List<ReflectiveClassBuildItem> oldClasses) {
        List<io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem> newClasses = new ArrayList<>();
        for (ReflectiveClassBuildItem oldClass : oldClasses) {
            String[] classNames = oldClass.getClassNames().toArray(new String[oldClass.getClassNames().size()]);
            if (oldClass.isWeak()) {
                newClasses.add(io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem.weakClass(classNames));
            } else {
                io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem.Builder builder = io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem
                        .builder(classNames)
                        .constructors(oldClass.isConstructors())
                        .methods(oldClass.isMethods())
                        .fields(oldClass.isFields())
                        .finalFieldsWritable(oldClass.areFinalFieldsWritable());
                newClasses.add(builder.build());
            }
        }
        return newClasses;
    }

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem> reflectiveFields(
            List<ReflectiveFieldBuildItem> oldFields) {
        List<io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem> newFields = new ArrayList<>();
        for (ReflectiveFieldBuildItem oldField : oldFields) {
            if (oldField.getFieldInfo() != null) {
                newFields
                        .add(new io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem(oldField.getFieldInfo()));
            } else if (oldField.getField() != null) {
                newFields.add(new io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem(oldField.getField()));
            } else {
                throw new RuntimeException(
                        "Deprecated " + ReflectiveFieldBuildItem.class.getSimpleName() + " conversion failed");
            }
        }
        return newFields;
    }

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem> reflectiveHierarchy(
            List<ReflectiveHierarchyBuildItem> oldHierarchies) {
        List<io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem> newHierarchies = new ArrayList<>();
        for (ReflectiveHierarchyBuildItem oldHierarchy : oldHierarchies) {
            newHierarchies.add(new io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem(
                    oldHierarchy.getType(), oldHierarchy.getIndex()));
        }
        return newHierarchies;
    }

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem> reflectiveHierarchyIgnoreWarning(
            List<ReflectiveHierarchyIgnoreWarningBuildItem> oldHierarchies) {
        List<io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem> newHierarchies = new ArrayList<>();
        for (ReflectiveHierarchyIgnoreWarningBuildItem oldHierarchy : oldHierarchies) {
            newHierarchies.add(new io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem(
                    oldHierarchy.getDotName()));
        }
        return newHierarchies;
    }

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem> reflectiveMethods(
            List<ReflectiveMethodBuildItem> oldMethods) {
        List<io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem> newMethods = new ArrayList<>();
        for (ReflectiveMethodBuildItem oldMethod : oldMethods) {
            if (oldMethod.getMethodInfo() != null) {
                newMethods.add(
                        new io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem(oldMethod.getMethodInfo()));
            } else if (oldMethod.getMethod() != null) {
                newMethods
                        .add(new io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem(oldMethod.getMethod()));
            } else {
                throw new RuntimeException(
                        "Deprecated " + ReflectiveMethodBuildItem.class.getSimpleName() + " conversion failed");
            }
        }
        return newMethods;
    }

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem> runtimeInitializedClass(
            List<RuntimeInitializedClassBuildItem> oldClasses) {
        List<io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem> newClasses = new ArrayList<>();
        for (RuntimeInitializedClassBuildItem oldClass : oldClasses) {
            newClasses.add(
                    new io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem(oldClass.getClassName()));
        }
        return newClasses;
    }

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem> runtimeReinitializedClass(
            List<RuntimeReinitializedClassBuildItem> oldClasses) {
        List<io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem> newClasses = new ArrayList<>();
        for (RuntimeReinitializedClassBuildItem oldClass : oldClasses) {
            newClasses.add(new io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem(
                    oldClass.getClassName()));
        }
        return newClasses;
    }

    @BuildStep
    List<io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem> serviceProviders(
            List<ServiceProviderBuildItem> oldProviders) {
        List<io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem> newProviders = new ArrayList<>();
        for (ServiceProviderBuildItem oldProvider : oldProviders) {
            newProviders.add(new io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem(
                    oldProvider.getServiceInterface(), oldProvider.providers()));
        }
        return newProviders;
    }

    @BuildStep
    List<NativeImageConfigBuildItem> substrateConfigs(List<SubstrateConfigBuildItem> oldConfigs) {
        List<NativeImageConfigBuildItem> newConfigs = new ArrayList<>();
        for (SubstrateConfigBuildItem oldConfig : oldConfigs) {
            NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder();
            for (String clazz : oldConfig.getRuntimeInitializedClasses()) {
                builder.addRuntimeInitializedClass(clazz);
            }
            for (String clazz : oldConfig.getRuntimeReinitializedClasses()) {
                builder.addRuntimeReinitializedClass(clazz);
            }
            for (String bundle : oldConfig.getResourceBundles()) {
                builder.addResourceBundle(bundle);
            }
            for (List<String> definition : oldConfig.getProxyDefinitions()) {
                builder.addProxyClassDefinition(definition.toArray(new String[definition.size()]));
            }
            for (Map.Entry<String, String> prop : oldConfig.getNativeImageSystemProperties().entrySet()) {
                builder.addNativeImageSystemProperty(prop.getKey(), prop.getValue());
            }
            newConfigs.add(builder.build());
        }
        return newConfigs;
    }

    @BuildStep
    List<NativeImageProxyDefinitionBuildItem> substrateProxyDefinitions(
            List<SubstrateProxyDefinitionBuildItem> oldDefinitions) {
        List<NativeImageProxyDefinitionBuildItem> newDefinitions = new ArrayList<>();
        for (SubstrateProxyDefinitionBuildItem oldDefinition : oldDefinitions) {
            newDefinitions.add(new NativeImageProxyDefinitionBuildItem(oldDefinition.getClasses()));
        }
        return newDefinitions;
    }

    @BuildStep
    List<NativeImageResourceBuildItem> substrateResources(List<SubstrateResourceBuildItem> oldResources) {
        List<NativeImageResourceBuildItem> newResources = new ArrayList<>();
        for (SubstrateResourceBuildItem oldResource : oldResources) {
            newResources.add(new NativeImageResourceBuildItem(oldResource.getResources()));
        }
        return newResources;
    }

    @BuildStep
    List<NativeImageResourceBundleBuildItem> substrateResourceBundles(List<SubstrateResourceBundleBuildItem> oldBundles) {
        List<NativeImageResourceBundleBuildItem> newBundles = new ArrayList<>();
        for (SubstrateResourceBundleBuildItem oldBundle : oldBundles) {
            newBundles.add(new NativeImageResourceBundleBuildItem(oldBundle.getBundleName()));
        }
        return newBundles;
    }

    @BuildStep
    List<NativeImageSystemPropertyBuildItem> substrateSystemProperties(List<SubstrateSystemPropertyBuildItem> oldProps) {
        List<NativeImageSystemPropertyBuildItem> newProps = new ArrayList<>();
        for (SubstrateSystemPropertyBuildItem oldProp : oldProps) {
            newProps.add(new NativeImageSystemPropertyBuildItem(oldProp.getKey(), oldProp.getValue()));
        }
        return newProps;
    }

    @BuildStep
    void enableAllCharsets(
            List<NativeEnableAllCharsetsBuildItem> oldProps,
            BuildProducer<NativeImageEnableAllCharsetsBuildItem> newProps) {
        if (!oldProps.isEmpty()) {
            newProps.produce(new NativeImageEnableAllCharsetsBuildItem());
        }
    }
}
