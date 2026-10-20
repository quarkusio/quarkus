package io.quarkus.hibernate.orm.deployment.bytecode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ConstantBootstrapBuildItem;
import io.quarkus.gizmo.TestClassLoader;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.ProcessorTestStubs;
import io.quarkus.hibernate.orm.packageinfo.withpackageinfo.EntityWithPackageInfo;
import io.quarkus.hibernate.orm.packageinfo.withpackageinfo.TestAnnotation;

class BytecodeProcessorAotTest {

    private final BytecodeProcessor processor = new BytecodeProcessor();

    @Test
    void generatesPackageInfoInterfaceClassForEachPackage() throws Exception {
        var model = ProcessorTestStubs.jpaModelWithManagedClasses(
                "org.acme.first.Book", "org.acme.first.Author", "org.acme.second.Store");

        GeneratedOutput output = generatePackageInfos(model, indexWithoutPackageInfo());

        assertThat(output.classes()).extracting(GeneratedClassBuildItem::binaryName)
                .containsExactlyInAnyOrder("org.acme.first.package-info", "org.acme.second.package-info");

        assertThat(output.classes()).allSatisfy(item -> {
            assertThat(item.isApplicationClass()).isTrue();
            Class<?> packageInfo = load(item);
            assertThat(packageInfo.isInterface()).isTrue();
            assertThat(packageInfo.isSynthetic()).isTrue();
        });

        assertThat(output.resources()).isEmpty();
        assertThat(output.serviceProviders()).isEmpty();
        assertThat(output.constantBootstraps()).isEmpty();
    }

    @Test
    void preservesAnExistingPackageInfo() throws Exception {
        // src/test/java/io/quarkus/hibernate/orm/packageinfo/withpackageinfo/package-info.java
        Class<?> existingPackageInfo = Class.forName(EntityWithPackageInfo.class.getPackageName() + ".package-info");

        var model = ProcessorTestStubs.jpaModelWithManagedClasses(EntityWithPackageInfo.class.getName());

        GeneratedOutput output = generatePackageInfos(model, Index.of(existingPackageInfo));

        assertThat(output.classes()).isEmpty();
        assertThat(existingPackageInfo).hasAnnotation(TestAnnotation.class);
        assertThat(existingPackageInfo.getAnnotation(TestAnnotation.class).value()).isEqualTo("original");
    }

    @Test
    void generatesNothingWhenThereAreNoManagedClassPackages() throws IOException {
        var model = ProcessorTestStubs.emptyJpaModel();

        GeneratedOutput output = generatePackageInfos(model, indexWithoutPackageInfo());

        assertThat(output.classes()).isEmpty();
        assertThat(output.resources()).isEmpty();
        assertThat(output.serviceProviders()).isEmpty();
        assertThat(output.constantBootstraps()).isEmpty();
    }

    private GeneratedOutput generatePackageInfos(JpaModelBuildItem model, Index existingClassesIndex) {
        List<GeneratedClassBuildItem> classes = new ArrayList<>();
        List<GeneratedResourceBuildItem> resources = new ArrayList<>();
        List<GeneratedServiceProviderBuildItem> serviceProviders = new ArrayList<>();
        List<ConstantBootstrapBuildItem> constantBootstraps = new ArrayList<>();

        processor.generateMissingPackageInfos(new CombinedIndexBuildItem(existingClassesIndex, existingClassesIndex), model,
                List.of(new ApplicationClassPredicateBuildItem(ignored -> true)), classes::add, resources::add,
                serviceProviders::add, constantBootstraps::add);

        return new GeneratedOutput(classes, resources, serviceProviders, constantBootstraps);
    }

    private static Index indexWithoutPackageInfo() {
        return new Indexer().complete();
    }

    private Class<?> load(GeneratedClassBuildItem generatedClass) {
        try {
            TestClassLoader classLoader = new TestClassLoader(getClass().getClassLoader());
            classLoader.write(generatedClass.binaryName(), generatedClass.getClassData());
            return classLoader.loadClass(generatedClass.binaryName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private record GeneratedOutput(List<GeneratedClassBuildItem> classes, List<GeneratedResourceBuildItem> resources,
            List<GeneratedServiceProviderBuildItem> serviceProviders, List<ConstantBootstrapBuildItem> constantBootstraps) {
    }
}
