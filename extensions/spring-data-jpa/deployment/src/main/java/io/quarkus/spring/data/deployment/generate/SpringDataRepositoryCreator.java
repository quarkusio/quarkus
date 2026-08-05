package io.quarkus.spring.data.deployment.generate;

import java.lang.constant.ClassDesc;
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
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
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

    public SpringDataRepositoryCreator(ClassOutput classOutput,
            ClassOutput otherClassOutput, IndexView index,
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

    public Result implementCrudRepository(ClassInfo repositoryToImplement, IndexView indexView) {
        Map.Entry<DotName, DotName> extraTypesResult = extractIdAndEntityTypes(repositoryToImplement, indexView);

        DotName idTypeDotName = extraTypesResult.getKey();
        String idTypeStr = idTypeDotName.toString();
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

        Map<String, FieldDesc> fragmentImplNameToFieldDescriptor = new HashMap<>();
        String repositoryToImplementStr = repositoryToImplement.name().toString();
        String generatedClassName = repositoryToImplementStr + "_" + HashUtil.sha1(repositoryToImplementStr) + "Impl";

        // Track existing methods across all adders
        Set<String> existingMethods = new HashSet<>();

        Gizmo gizmo = Gizmo.create(classOutput)
                .withDebugInfo(false)
                .withParameters(false);
        gizmo.class_(generatedClassName, classCreator -> {
            classCreator.implements_(ClassDesc.of(repositoryToImplementStr));
            classCreator.addAnnotation(ApplicationScoped.class);

            // Create the entityClass field
            FieldDesc entityClassFieldDesc = classCreator.field("entityClass", ifc -> {
                ifc.setType(Class.class);
                ifc.private_();
                ifc.final_();
            });

            // create an instance field of type Class for each one of the implementations of the custom interfaces
            createCustomImplFields(classCreator, fragmentNamesToImplement, index, fragmentImplNameToFieldDescriptor);

            // initialize all class fields in the constructor
            classCreator.constructor(ctor -> {
                ctor.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(Object.class), ctor.this_());
                    // initialize the entityClass field
                    bc.set(ctor.this_().field(entityClassFieldDesc),
                            Const.of(ClassDesc.of(entityTypeStr)));
                    bc.return_();
                });
            });

            // for every method we add we need to make sure that we only haven't added it before
            // we first add custom methods (as per Spring Data implementation) thus ensuring that user provided methods
            // always override stock methods from the Spring Data repository interfaces

            fragmentMethodsAdder.add(classCreator, generatedClassName, fragmentNamesToImplement,
                    fragmentImplNameToFieldDescriptor, existingMethods);

            stockMethodsAdder.add(classCreator, entityClassFieldDesc, generatedClassName,
                    repositoryToImplement, entityDotName, idTypeStr, existingMethods);
            derivedMethodsAdder.add(classCreator, entityClassFieldDesc, generatedClassName,
                    repositoryToImplement, entityClassInfo, existingMethods);
            customQueryMethodsAdder.add(classCreator, entityClassFieldDesc,
                    repositoryToImplement, entityClassInfo, idTypeStr, existingMethods);
        });

        return new Result(entityDotName, idTypeDotName, generatedClassName);
    }

    private Map.Entry<DotName, DotName> extractIdAndEntityTypes(ClassInfo repositoryToImplement, IndexView indexView) {
        AnnotationInstance repositoryDefinitionInstance = repositoryToImplement
                .declaredAnnotation(DotNames.SPRING_DATA_REPOSITORY_DEFINITION);
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

    private void createCustomImplFields(io.quarkus.gizmo2.creator.ClassCreator repositoryImpl,
            List<DotName> customInterfaceNamesToImplement,
            IndexView index, Map<String, FieldDesc> customImplNameToFieldDescriptor) {
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
            final int fieldIndex = i;
            FieldDesc customClassField = repositoryImpl.field("customImplClass" + (fieldIndex + 1), ifc -> {
                ifc.setType(ClassDesc.of(customImplClassName));
                ifc.protected_(); // done to prevent warning during the build
                ifc.addAnnotation(Inject.class);
            });

            customImplNameToFieldDescriptor.put(customImplClassName, customClassField);
            i++;
        }
    }

    public static final class Result {
        final DotName entityDotName;
        final DotName idTypeDotName;
        final String generatedClassName;

        Result(DotName entityDotName, DotName idTypeDotName, String generatedClassName) {
            this.entityDotName = entityDotName;
            this.idTypeDotName = idTypeDotName;
            this.generatedClassName = generatedClassName;
        }

        public DotName getEntityDotName() {
            return entityDotName;
        }

        public DotName getIdTypeDotName() {
            return idTypeDotName;
        }

        public String getGeneratedClassName() {
            return generatedClassName;
        }
    }
}
