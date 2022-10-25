package io.quarkus.spring.data.deployment.generate;

import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.spring.data.deployment.DotNames;

public class SpringDataRepositoryCreator {

    private final ClassOutput classOutput;
    private final IndexView index;
    private final FragmentMethodsAdder fragmentMethodsAdder;
    private final StockMethodsAdder stockMethodsAdder;
    private final DerivedMethodsAdder derivedMethodsAdder;
    private final CustomQueryMethodsAdder customQueryMethodsAdder;

    public SpringDataRepositoryCreator(ClassOutput classOutput, ClassOutput otherClassOutput, IndexView index,
            Consumer<String> fragmentImplClassResolvedCallback,
            Consumer<String> customClassCreatedCallback, TypeBundle typeBundle) {
        this.classOutput = classOutput;
        this.index = index;
        this.fragmentMethodsAdder = new FragmentMethodsAdder(fragmentImplClassResolvedCallback, index);
        this.stockMethodsAdder = new StockMethodsAdder(index, typeBundle);
        this.derivedMethodsAdder = new DerivedMethodsAdder(index, typeBundle, otherClassOutput, customClassCreatedCallback);

        // custom queries may generate non-bean classes
        this.customQueryMethodsAdder = new CustomQueryMethodsAdder(index, otherClassOutput, customClassCreatedCallback,
                typeBundle);
    }

    public void implementCrudRepository(ClassInfo repositoryToImplement, IndexView indexView) {
        Map.Entry<DotName, DotName> extraTypesResult = extractIdAndEntityTypes(repositoryToImplement, indexView);

        String idTypeStr = extraTypesResult.getKey().toString();
        DotName entityDotName = extraTypesResult.getValue();
        String entityTypeStr = entityDotName.toString();

        ClassInfo entityClassInfo = index.getClassByName(entityDotName);
        if (entityClassInfo == null) {
            throw new IllegalStateException("Entity " + entityDotName + " was not part of the Quarkus index");
        }

        // handle the fragment repositories
        // Spring Data allows users to define (and implement their own interfaces containing data access related methods)
        // that can then be used along with any of the typical Spring Data repository interfaces in the final
        // repository in order to compose functionality

        List<DotName> interfaceNames = repositoryToImplement.interfaceNames();
        List<DotName> fragmentNamesToImplement = new ArrayList<>(interfaceNames.size());
        for (DotName interfaceName : interfaceNames) {
            if (!DotNames.SUPPORTED_REPOSITORIES.contains(interfaceName)
                    && !GenerationUtil.isIntermediateRepository(interfaceName, indexView)) {
                fragmentNamesToImplement.add(interfaceName);
            }
        }

        Map<String, FieldDescriptor> fragmentImplNameToFieldDescriptor = new HashMap<>();
        String repositoryToImplementStr = repositoryToImplement.name().toString();
        String generatedClassName = repositoryToImplementStr + "_" + HashUtil.sha1(repositoryToImplementStr) + "Impl";
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(generatedClassName)
                .interfaces(repositoryToImplementStr)
                .build()) {
            classCreator.addAnnotation(ApplicationScoped.class);

            FieldCreator entityClassFieldCreator = classCreator.getFieldCreator("entityClass", Class.class.getName())
                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL);

            // create an instance field of type Class for each one of the implementations of the custom interfaces
            createCustomImplFields(classCreator, fragmentNamesToImplement, index, fragmentImplNameToFieldDescriptor);

            // initialize all class fields in the constructor
            try (MethodCreator ctor = classCreator.getMethodCreator("<init>", "V")) {
                ctor.invokeSpecialMethod(MethodDescriptor.ofMethod(Object.class, "<init>", void.class), ctor.getThis());
                // initialize the entityClass field
                ctor.writeInstanceField(entityClassFieldCreator.getFieldDescriptor(), ctor.getThis(),
                        ctor.loadClassFromTCCL(entityTypeStr));
                ctor.returnValue(null);
            }

            // for every method we add we need to make sure that we only haven't added it before
            // we first add custom methods (as per Spring Data implementation) thus ensuring that user provided methods
            // always override stock methods from the Spring Data repository interfaces

            fragmentMethodsAdder.add(classCreator, generatedClassName, fragmentNamesToImplement,
                    fragmentImplNameToFieldDescriptor);

            stockMethodsAdder.add(classCreator, entityClassFieldCreator.getFieldDescriptor(), generatedClassName,
                    repositoryToImplement, entityDotName, idTypeStr);
            derivedMethodsAdder.add(classCreator, entityClassFieldCreator.getFieldDescriptor(), generatedClassName,
                    repositoryToImplement, entityClassInfo);
            customQueryMethodsAdder.add(classCreator, entityClassFieldCreator.getFieldDescriptor(),
                    repositoryToImplement, entityClassInfo, idTypeStr);
        }
    }

    private Map.Entry<DotName, DotName> extractIdAndEntityTypes(ClassInfo repositoryToImplement, IndexView indexView) {
        AnnotationInstance repositoryDefinitionInstance = repositoryToImplement
                .classAnnotation(DotNames.SPRING_DATA_REPOSITORY_DEFINITION);
        if (repositoryDefinitionInstance != null) {
            return new AbstractMap.SimpleEntry<>(repositoryDefinitionInstance.value("idClass").asClass().name(),
                    repositoryDefinitionInstance.value("domainClass").asClass().name());
        }

        DotName entityDotName = null;
        DotName idDotName = null;

        // we need to pull the entity and ID types for the Spring Data generic types
        // we also need to make sure that the user didn't try to specify multiple different types
        // in the same interface (which is possible if only Repository is used)
        for (DotName extendedSpringDataRepo : GenerationUtil.extendedSpringDataRepos(repositoryToImplement, indexView)) {
            List<Type> types = JandexUtil.resolveTypeParameters(repositoryToImplement.name(), extendedSpringDataRepo, index);
            if (!(types.get(0) instanceof ClassType)) {
                throw new IllegalArgumentException(
                        "Entity generic argument of " + repositoryToImplement + " is not a regular class type");
            }
            DotName newEntityDotName = types.get(0).name();
            if ((entityDotName != null) && !newEntityDotName.equals(entityDotName)) {
                throw new IllegalArgumentException("Repository " + repositoryToImplement + " specifies multiple Entity types");
            }
            entityDotName = newEntityDotName;

            DotName newIdDotName = types.get(1).name();
            if ((idDotName != null) && !newIdDotName.equals(idDotName)) {
                throw new IllegalArgumentException("Repository " + repositoryToImplement + " specifies multiple ID types");
            }
            idDotName = newIdDotName;
        }

        if (idDotName == null || entityDotName == null) {
            throw new IllegalArgumentException(
                    "Repository " + repositoryToImplement + " does not specify ID and/or Entity type");
        }

        return new AbstractMap.SimpleEntry<>(idDotName, entityDotName);
    }

    private void createCustomImplFields(ClassCreator repositoryImpl, List<DotName> customInterfaceNamesToImplement,
            IndexView index, Map<String, FieldDescriptor> customImplNameToFieldDescriptor) {
        Set<String> customImplClassNames = new HashSet<>(customInterfaceNamesToImplement.size());

        // go through the interfaces and collect the implementing classes in a Set
        // this is done because it is possible for an implementing class to implement multiple fragments
        for (DotName customInterfaceToImplement : customInterfaceNamesToImplement) {
            customImplClassNames
                    .add(FragmentMethodsUtil.getImplementationDotName(customInterfaceToImplement, index).toString());
        }

        // do the actual field creation and book-keeping of them in the customImplNameToFieldDescriptor Map
        int i = 0;
        for (String customImplClassName : customImplClassNames) {
            FieldCreator customClassField = repositoryImpl
                    .getFieldCreator("customImplClass" + (i + 1), customImplClassName)
                    .setModifiers(Modifier.PROTECTED); // done to prevent warning during the build
            customClassField.addAnnotation(Inject.class);

            customImplNameToFieldDescriptor.put(customImplClassName,
                    customClassField.getFieldDescriptor());
            i++;
        }
    }
}
