package io.quarkus.xmlsecurity.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class XmlsecurityProcessor {

  private static final String FEATURE = "xmlsecurity";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  @BuildStep
  ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
    return new ExtensionSslNativeSupportBuildItem(FEATURE);
  }

  @BuildStep
  IndexDependencyBuildItem indexDependencies() {
    return new IndexDependencyBuildItem("org.apache.santuario", "xmlsec");
  }

  @BuildStep
  void
  registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                        CombinedIndexBuildItem combinedIndex) {
    IndexView index = combinedIndex.getIndex();

    Stream.of(CanonicalizerSpi.class, TransformSpi.class)
        .map(aClass -> aClass.getName())
        .map(DotName::createSimple)
        .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
        .map(classInfo -> classInfo.name().toString())
        .map(className -> new ReflectiveClassBuildItem(false, false, className))
        .forEach(reflectiveClass::produce);

    Stream.of(GCMParameterSpec.class.getName(), XPathType[].class.getName())
        .map(className -> new ReflectiveClassBuildItem(false, false, className))
        .forEach(reflectiveClass::produce);
  }
}
