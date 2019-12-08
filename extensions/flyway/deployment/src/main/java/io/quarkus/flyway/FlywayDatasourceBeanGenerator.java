package io.quarkus.flyway;

import java.util.Collection;
import java.util.HashSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.flyway.runtime.FlywayProducer;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * Generates the CDI producer bean for {@link Flyway} at build time.<br>
 * Supports multiple {@link Named} {@link DataSource}s.
 * <p>
 * It produces {@link Flyway} instances for every {@link Named} {@link DataSource}.
 * <p>
 * All {@link Flyway} instances get named the same way as the {@link DataSource}s,
 * prepended by the prefix {@value #FLYWAY_BEAN_NAME_PREFIX}.
 */
class FlywayDatasourceBeanGenerator {

    public static final String FLYWAY_BEAN_NAME_PREFIX = "flyway_";

    private static final String FLYWAY_PRODUCER_BEAN_NAME = "FlywayDataSourceProducer";
    private static final String FLYWAY_PRODUCER_PACKAGE_NAME = FlywayProducer.class.getPackage().getName();
    private static final String FLYWAY_PRODUCER_TYPE_NAME = FLYWAY_PRODUCER_PACKAGE_NAME + "." + FLYWAY_PRODUCER_BEAN_NAME;

    private static final int ACCESS_PACKAGE_PROTECTED = 0;

    private final Collection<String> dataSourceNames = new HashSet<>();
    private final BuildProducer<GeneratedBeanBuildItem> generatedBean;

    public FlywayDatasourceBeanGenerator(Collection<String> dataSourceNames,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) {
        this.dataSourceNames.addAll(dataSourceNames);
        this.generatedBean = generatedBean;
    }

    /**
     * Create a producer bean managing flyway.
     * <p>
     * Build time and runtime configuration are both injected into this bean.
     *
     * @return String name of the generated producer bean class.
     */
    public void createFlywayProducerBean() {
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(this::writeGeneratedBeanBuildItem)
                .className(FLYWAY_PRODUCER_TYPE_NAME)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        FieldCreator defaultProducerField = classCreator.getFieldCreator("defaultProducer", FlywayProducer.class);
        defaultProducerField.setModifiers(ACCESS_PACKAGE_PROTECTED);
        defaultProducerField.addAnnotation(Inject.class);

        for (String dataSourceName : dataSourceNames) {
            String dataSourceFieldName = "dataSource" + hashed(dataSourceName);
            FieldCreator dataSourceField = classCreator.getFieldCreator(dataSourceFieldName, DataSource.class);
            dataSourceField.setModifiers(ACCESS_PACKAGE_PROTECTED);
            dataSourceField.addAnnotation(Inject.class);
            dataSourceField.addAnnotation(annotatedWithNamed(dataSourceName));

            String producerMethodName = "createFlywayForDataSource" + hashed(dataSourceName);
            MethodCreator flywayProducerMethod = classCreator.getMethodCreator(producerMethodName, Flyway.class);
            flywayProducerMethod.addAnnotation(Produces.class);
            flywayProducerMethod.addAnnotation(Dependent.class);
            flywayProducerMethod.addAnnotation(annotatedWithFlywayDatasource(dataSourceName));
            flywayProducerMethod.addAnnotation(annotatedWithNamed(FLYWAY_BEAN_NAME_PREFIX + dataSourceName));

            flywayProducerMethod.returnValue(
                    flywayProducerMethod.invokeVirtualMethod(
                            createFlywayMethod(),
                            resultHandleFor(defaultProducerField, flywayProducerMethod),
                            resultHandleFor(dataSourceField, flywayProducerMethod),
                            flywayProducerMethod.load(dataSourceName)));
        }
        classCreator.close();
    }

    private void writeGeneratedBeanBuildItem(String name, byte[] data) {
        generatedBean.produce(new GeneratedBeanBuildItem(name, data));
    }

    private static String hashed(String dataSourceName) {
        return "_" + HashUtil.sha1(dataSourceName);
    }

    private static MethodDescriptor createFlywayMethod() {
        Class<?>[] parameterTypes = { DataSource.class, String.class };
        return MethodDescriptor.ofMethod(FlywayProducer.class, "createFlyway", Flyway.class, parameterTypes);
    }

    private static ResultHandle resultHandleFor(FieldCreator field, BytecodeCreator method) {
        FieldDescriptor fieldDescriptor = field.getFieldDescriptor();
        return method.readInstanceField(fieldDescriptor, method.getThis());
    }

    private static AnnotationInstance annotatedWithNamed(String dataSourceName) {
        return AnnotationInstance.create(DotNames.NAMED, null,
                new AnnotationValue[] { AnnotationValue.createStringValue("value", dataSourceName) });
    }

    //Since is does not seem to be possible to generate the annotation "@Typed",
    //because AnnotationValue.createArrayValue is not implemented yet (jandex, August 2019),
    //the annotation "@FlywayDataSource" was introduced (in conformity with @DataSource).
    private AnnotationInstance annotatedWithFlywayDatasource(String dataSourceName) {
        return AnnotationInstance.create(DotName.createSimple(FlywayDataSource.class.getName()), null,
                new AnnotationValue[] { AnnotationValue.createStringValue("value", dataSourceName) });
    }

    @Override
    public String toString() {
        return "FlywayDatasourceBeanGenerator [dataSourceNames=" + dataSourceNames + ", generatedBean=" + generatedBean + "]";
    }
}