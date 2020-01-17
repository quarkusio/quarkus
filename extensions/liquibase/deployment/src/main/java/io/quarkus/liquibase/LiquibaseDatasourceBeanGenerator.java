package io.quarkus.liquibase;

import java.util.Collection;
import java.util.HashSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.liquibase.runtime.LiquibaseProducer;

/**
 * Generates the CDI producer bean for {@link LiquibaseFactory} at build time.<br>
 * Supports multiple named {@link DataSource}s.
 * <p>
 * It produces {@link LiquibaseFactory} instances for every {@link Named} {@link DataSource}.
 * <p>
 * All {@link LiquibaseFactory} instances get named the same way as the {@link DataSource}s,
 * prepended by the prefix {@value #LIQUIBASE_BEAN_NAME_PREFIX}.
 */
class LiquibaseDatasourceBeanGenerator {

    public static final String LIQUIBASE_BEAN_NAME_PREFIX = "liquibase_";

    private static final String LIQUIBASE_PRODUCER_BEAN_NAME = "LiquibaseDataSourceProducer";
    private static final String LIQUIBASE_PRODUCER_PACKAGE_NAME = LiquibaseProducer.class.getPackage().getName();
    private static final String LIQUIBASE_PRODUCER_TYPE_NAME = LIQUIBASE_PRODUCER_PACKAGE_NAME + "."
            + LIQUIBASE_PRODUCER_BEAN_NAME;

    private static final int ACCESS_PACKAGE_PROTECTED = 0;

    private final Collection<String> dataSourceNames = new HashSet<>();
    private final BuildProducer<GeneratedBeanBuildItem> generatedBean;

    public LiquibaseDatasourceBeanGenerator(Collection<String> dataSourceNames,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) {
        this.dataSourceNames.addAll(dataSourceNames);
        this.generatedBean = generatedBean;
    }

    /**
     * Create a producer bean managing liquibase.
     * <p>
     * Build time and runtime configuration are both injected into this bean.
     *
     * @return String name of the generated producer bean class.
     */
    public void createLiquibaseProducerBean() {
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(this::writeGeneratedBeanBuildItem)
                .className(LIQUIBASE_PRODUCER_TYPE_NAME)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        FieldCreator defaultProducerField = classCreator.getFieldCreator("defaultProducer", LiquibaseProducer.class);
        defaultProducerField.setModifiers(ACCESS_PACKAGE_PROTECTED);
        defaultProducerField.addAnnotation(Inject.class);

        for (String dataSourceName : dataSourceNames) {
            String dataSourceFieldName = "dataSource" + hashed(dataSourceName);
            FieldCreator dataSourceField = classCreator.getFieldCreator(dataSourceFieldName, DataSource.class);
            dataSourceField.setModifiers(ACCESS_PACKAGE_PROTECTED);
            dataSourceField.addAnnotation(Inject.class);
            dataSourceField.addAnnotation(annotatedWithNamed(dataSourceName));

            String producerMethodName = "createLiquibaseForDataSource" + hashed(dataSourceName);
            MethodCreator liquibaseProducerMethod = classCreator.getMethodCreator(producerMethodName, LiquibaseFactory.class);
            liquibaseProducerMethod.addAnnotation(Produces.class);
            liquibaseProducerMethod.addAnnotation(Dependent.class);
            liquibaseProducerMethod.addAnnotation(annotatedWithLiquibaseDatasource(dataSourceName));
            liquibaseProducerMethod.addAnnotation(annotatedWithNamed(LIQUIBASE_BEAN_NAME_PREFIX + dataSourceName));

            liquibaseProducerMethod.returnValue(
                    liquibaseProducerMethod.invokeVirtualMethod(
                            createLiquibaseMethod(),
                            resultHandleFor(defaultProducerField, liquibaseProducerMethod),
                            resultHandleFor(dataSourceField, liquibaseProducerMethod),
                            liquibaseProducerMethod.load(dataSourceName)));
        }
        classCreator.close();
    }

    private void writeGeneratedBeanBuildItem(String name, byte[] data) {
        generatedBean.produce(new GeneratedBeanBuildItem(name, data));
    }

    private static String hashed(String dataSourceName) {
        return "_" + HashUtil.sha1(dataSourceName);
    }

    private static MethodDescriptor createLiquibaseMethod() {
        Class<?>[] parameterTypes = { AgroalDataSource.class, String.class };
        return MethodDescriptor.ofMethod(LiquibaseProducer.class, "createLiquibase", LiquibaseFactory.class, parameterTypes);
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
    //the annotation "@LiquibaseDataSource" was introduced (in conformity with @DataSource).
    private AnnotationInstance annotatedWithLiquibaseDatasource(String dataSourceName) {
        return AnnotationInstance.create(DotName.createSimple(LiquibaseDataSource.class.getName()), null,
                new AnnotationValue[] { AnnotationValue.createStringValue("value", dataSourceName) });
    }

    @Override
    public String toString() {
        return "LiquibaseDatasourceBeanGenerator [dataSourceNames=" + dataSourceNames + ", generatedBean=" + generatedBean
                + "]";
    }
}
