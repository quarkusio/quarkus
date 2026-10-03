package io.quarkus.data.processor;

import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.hibernate.processor.util.StringUtil.decapitalize;
import static org.hibernate.processor.util.TypeUtils.containsAnnotation;
import static org.hibernate.processor.util.TypeUtils.extendsClass;
import static org.hibernate.processor.util.TypeUtils.hasAnnotation;
import static org.hibernate.processor.util.TypeUtils.implementsInterface;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import jakarta.annotation.Nullable;

import org.hibernate.processor.spi.AnnotationMetaEntityContext;
import org.hibernate.processor.spi.HibernateProcessorExtension;
import org.hibernate.processor.spi.SessionSetup;

public class QuarkusDataHibernateExtension implements HibernateProcessorExtension {

    // Panache 1 constants
    private static final String PANACHE_ORM_REPOSITORY_BASE = "io.quarkus.hibernate.orm.panache.PanacheRepositoryBase";
    private static final String PANACHE_ORM_ENTITY_BASE = "io.quarkus.hibernate.orm.panache.PanacheEntityBase";
    private static final String PANACHE_REACTIVE_REPOSITORY_BASE = "io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase";
    private static final String PANACHE_REACTIVE_ENTITY_BASE = "io.quarkus.hibernate.reactive.panache.PanacheEntityBase";

    private static final String ENTITY = "jakarta.persistence.Entity";

    // Quarkus Data constants
    private static final String ENTITY_MARKER = "io.quarkus.data.hibernate.EntitySwitcher";
    private static final String MANAGED_BLOCKING_REPOSITORY_BASE = "io.quarkus.data.hibernate.managed.blocking.BlockingManagedRepositoryBase";
    private static final String RECORD_BLOCKING_REPOSITORY_BASE = "io.quarkus.data.hibernate.record.blocking.BlockingRecordRepositoryBase";
    private static final String MANAGED_REACTIVE_REPOSITORY_BASE = "io.quarkus.data.hibernate.managed.reactive.ReactiveManagedRepositoryBase";
    private static final String RECORD_REACTIVE_REPOSITORY_BASE = "io.quarkus.data.hibernate.record.reactive.ReactiveRecordRepositoryBase";

    // Session constants
    private static final String QUARKUS_SESSION_OPERATIONS = "io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations";
    private static final String HIB_SESSION = "org.hibernate.Session";
    private static final String HIB_STATELESS_SESSION = "org.hibernate.StatelessSession";
    private static final String MUTINY_SESSION = "org.hibernate.reactive.mutiny.Mutiny.Session";
    private static final String MUTINY_STATELESS_SESSION = "org.hibernate.reactive.mutiny.Mutiny.StatelessSession";
    private static final String UNI = "io.smallrye.mutiny.Uni";
    private static final String UNI_MUTINY_SESSION = UNI + "<" + MUTINY_SESSION + ">";
    private static final String UNI_MUTINY_STATELESS_SESSION = UNI + "<" + MUTINY_STATELESS_SESSION + ">";

    // Annotation constants
    private static final String HQL = "org.hibernate.annotations.processing.HQL";
    private static final String SQL = "org.hibernate.annotations.processing.SQL";
    private static final String JD_QUERY = "jakarta.data.repository.Query";
    private static final String FIND = "org.hibernate.annotations.processing.Find";
    private static final String JD_FIND = "jakarta.data.repository.Find";

    // Classpath detection state
    private boolean usesQuarkusOrm;
    private boolean usesQuarkusReactive;
    private boolean usesQuarkusDataHibernate;
    private boolean usesQuarkusReactiveCommon;
    private boolean injectionAvailable;

    @Override
    public void init(ProcessingEnvironment processingEnvironment) {
        final var elements = processingEnvironment.getElementUtils();

        final var quarkusOrmPackage = elements.getPackageElement("io.quarkus.hibernate.orm");
        final var quarkusReactivePackage = elements.getPackageElement("io.quarkus.hibernate.reactive.runtime");

        var quarkusOrmPanachePackage = elements.getPackageElement("io.quarkus.hibernate.orm.panache");
        var quarkusReactivePanachePackage = elements.getPackageElement("io.quarkus.hibernate.reactive.panache");
        var quarkusReactivePanacheCommonPackage = elements.getPackageElement("io.quarkus.hibernate.reactive.panache.common");
        var quarkusDataHibernatePackage = elements.getPackageElement("io.quarkus.data.hibernate");

        if (packagePresent(quarkusReactivePanachePackage)
                && packagePresent(quarkusOrmPanachePackage)) {
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Both Quarkus Hibernate ORM and Hibernate Reactive with Panache detected:"
                            + " this is not supported, so will proceed as if none were there");
            quarkusOrmPanachePackage = null;
            quarkusReactivePanachePackage = null;
        }

        injectionAvailable = packagePresent(quarkusOrmPackage) || packagePresent(quarkusReactivePackage);
        usesQuarkusOrm = packagePresent(quarkusOrmPanachePackage);
        usesQuarkusReactive = packagePresent(quarkusReactivePanachePackage);
        usesQuarkusDataHibernate = packagePresent(quarkusDataHibernatePackage);
        usesQuarkusReactiveCommon = packagePresent(quarkusReactivePanacheCommonPackage);
    }

    private static boolean packagePresent(@Nullable PackageElement pack) {
        return pack != null && !pack.getEnclosedElements().isEmpty();
    }

    @Override
    public @Nullable String qualifierAnnotation() {
        return injectionAvailable ? "io.quarkus.hibernate.orm.PersistenceUnit" : null;
    }

    // --- Type classification ---

    @Override
    public boolean isExtensionEntity(TypeElement type) {
        return isPanacheType(type) || isQuarkusDataType(type);
    }

    @Override
    public boolean isExtensionRepository(TypeElement type) {
        return isQuarkusDataRepository(type);
    }

    private boolean isPanacheType(TypeElement type) {
        return usesQuarkusOrm && isOrmPanacheType(type)
                || usesQuarkusReactive && isReactivePanacheType(type);
    }

    private static boolean isOrmPanacheType(TypeElement type) {
        return implementsInterface(type, PANACHE_ORM_REPOSITORY_BASE)
                || extendsClass(type, PANACHE_ORM_ENTITY_BASE);
    }

    private static boolean isReactivePanacheType(TypeElement type) {
        return implementsInterface(type, PANACHE_REACTIVE_REPOSITORY_BASE)
                || extendsClass(type, PANACHE_REACTIVE_ENTITY_BASE);
    }

    private boolean isQuarkusDataType(TypeElement type) {
        return implementsInterface(type, ENTITY_MARKER) || isQuarkusDataRepository(type);
    }

    private boolean isQuarkusDataRepository(TypeElement type) {
        return implementsInterface(type, MANAGED_BLOCKING_REPOSITORY_BASE)
                || implementsInterface(type, RECORD_BLOCKING_REPOSITORY_BASE)
                || implementsInterface(type, MANAGED_REACTIVE_REPOSITORY_BASE)
                || implementsInterface(type, RECORD_REACTIVE_REPOSITORY_BASE);
    }

    private boolean hasQuarkusDataEntitySuperType(TypeElement element) {
        var superClass = element.getSuperclass();
        while (superClass.getKind() == TypeKind.DECLARED) {
            final var superType = (TypeElement) ((DeclaredType) superClass).asElement();
            if (hasAnnotation(superType, ENTITY) && isQuarkusDataType(superType)) {
                return true;
            }
            superClass = superType.getSuperclass();
        }
        return false;
    }

    private boolean isQuarkusDataBlockingRepository(TypeElement type) {
        return implementsInterface(type, MANAGED_BLOCKING_REPOSITORY_BASE)
                || implementsInterface(type, RECORD_BLOCKING_REPOSITORY_BASE);
    }

    private boolean isRecordReactiveRepository(TypeElement type) {
        return implementsInterface(type, RECORD_REACTIVE_REPOSITORY_BASE);
    }

    private boolean isRecordBlockingRepository(TypeElement type) {
        return implementsInterface(type, RECORD_BLOCKING_REPOSITORY_BASE);
    }

    // --- Repository members ---

    @Override
    public void addRepositoryMembers(TypeElement element, AnnotationMetaEntityContext context) {
        if (!isQuarkusDataType(element)) {
            return;
        }
        Element managedRepository = null;
        Element recordRepository = null;
        Element managedReactiveRepository = null;
        Element recordReactiveRepository = null;
        final var nestedRepositories = new ArrayList<String>();

        for (var enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.INTERFACE) {
                if (!addUserDefinedRepositoryAccessor(context, element, enclosedElement, nestedRepositories)) {
                    continue;
                }
                if (implementsInterface((TypeElement) enclosedElement, MANAGED_BLOCKING_REPOSITORY_BASE)) {
                    managedRepository = enclosedElement;
                } else if (implementsInterface((TypeElement) enclosedElement, RECORD_BLOCKING_REPOSITORY_BASE)) {
                    recordRepository = enclosedElement;
                } else if (implementsInterface((TypeElement) enclosedElement, MANAGED_REACTIVE_REPOSITORY_BASE)) {
                    managedReactiveRepository = enclosedElement;
                } else if (implementsInterface((TypeElement) enclosedElement, RECORD_REACTIVE_REPOSITORY_BASE)) {
                    recordReactiveRepository = enclosedElement;
                }
            }
        }

        // Entity metamodels in an inheritance hierarchy extend the parent metamodel.
        // Quarkus Data default repository accessors have fixed names, but their generated
        // repository return types are entity-specific, so emitting them on subclasses
        // would produce invalid static method hiding.
        if (injectionAvailable && !hasQuarkusDataEntitySuperType(element)) {
            final var idType = context.findIdType();
            addAccessor(context, managedRepository, idType, "managed",
                    MANAGED_BLOCKING_REPOSITORY_BASE, nestedRepositories);
            addAccessor(context, recordRepository, idType, "record",
                    RECORD_BLOCKING_REPOSITORY_BASE, nestedRepositories);
            if (usesQuarkusReactiveCommon) {
                addAccessor(context, managedReactiveRepository, idType, "managedReactive",
                        MANAGED_REACTIVE_REPOSITORY_BASE, nestedRepositories);
                addAccessor(context, recordReactiveRepository, idType, "recordReactive",
                        RECORD_REACTIVE_REPOSITORY_BASE, nestedRepositories);
            }
        }
    }

    private boolean addUserDefinedRepositoryAccessor(
            AnnotationMetaEntityContext context, TypeElement entity,
            Element enclosedElement, List<String> nestedRepositories) {
        final var name = enclosedElement.getSimpleName().toString();
        if (name.endsWith("_")) {
            context.message(entity,
                    "Nested repositories may not have names that end with '_': "
                            + entity.getQualifiedName() + "." + name,
                    Diagnostic.Kind.ERROR);
            return false;
        }
        nestedRepositories.add(name + "_");
        final var propertyName = decapitalize(name);
        final var qualifiedName = ((TypeElement) enclosedElement).getQualifiedName().toString();
        context.addMember(propertyName,
                new CDIAccessorMetaAttribute(context.metamodel(), propertyName, qualifiedName));
        return true;
    }

    private void addAccessor(
            AnnotationMetaEntityContext context,
            @Nullable Element repositoryType, @Nullable TypeMirror idType,
            String accessorName, String repositoryBaseType, List<String> nestedRepositories) {
        final var primaryEntity = context.primaryEntity();
        if (repositoryType != null) {
            addRepositoryAccessor(context, accessorName,
                    ((TypeElement) repositoryType).getQualifiedName().toString());
        } else if (idType != null && primaryEntity != null) {
            final var typeName = repositoryTypeName(context, accessorName, nestedRepositories);
            final var superType = repositoryBaseType
                    + "<" + primaryEntity.getSimpleName() + ", " + idType + ">";
            context.addMember(typeName,
                    new CDITypeMetaAttribute(context.metamodel(), typeName, superType));
            addRepositoryAccessor(context, accessorName, typeName);
        }
    }

    private void addRepositoryAccessor(
            AnnotationMetaEntityContext context,
            String accessorName, String repositoryType) {
        if (!context.hasMember(accessorName)) {
            context.addMember(accessorName,
                    new CDIAccessorMetaAttribute(context.metamodel(), accessorName, repositoryType));
        } else {
            context.message(context.metamodel().getElement(),
                    "Failed to generate accessor in '" + context.primaryEntity()
                            + "' for the generated repository under name '" + accessorName
                            + "' since it is already defined by the user",
                    Diagnostic.Kind.WARNING);
        }
    }

    private String repositoryTypeName(
            AnnotationMetaEntityContext context,
            String accessorName, List<String> nestedRepositories) {
        final var typeName = "QuarkusData"
                + Character.toUpperCase(accessorName.charAt(0))
                + accessorName.substring(1)
                + "Repository_";
        return context.hasMember(typeName) || nestedRepositories.contains(typeName)
                ? typeName + "_"
                : typeName;
    }

    // --- Session setup ---

    @Override
    public @Nullable SessionSetup setupRepositorySession(
            TypeElement element,
            @Nullable ExecutableElement getter,
            AnnotationMetaEntityContext context) {

        final boolean entity = isExtensionEntity(element);
        final boolean repo = isExtensionRepository(element);

        if (getter != null) {
            if (!entity && !repo) {
                return null;
            }
            if (repo) {
                return new SessionSetup(doSetupRepositoryConstructor(getter, element, context), true);
            } else {
                return new SessionSetup(context.fullReturnType(getter), false);
            }
        }

        if (element.getKind() == ElementKind.INTERFACE
                && !context.isJakartaDataRepository()
                && isInQuarkusEnvironment()) {
            return new SessionSetup(doSetupRepositoryConstructor(null, element, context), true);
        }

        return null;
    }

    private boolean isInQuarkusEnvironment() {
        return usesQuarkusOrm || usesQuarkusReactive || usesQuarkusDataHibernate;
    }

    private String doSetupRepositoryConstructor(
            @Nullable ExecutableElement getter,
            @Nullable TypeElement element,
            AnnotationMetaEntityContext context) {
        if (isBlockingFavored(element, context)) {
            final var name = sessionGetterName(getter, element);
            final var sessionType = sessionType(getter, element, context);
            context.addRepositoryConstructor(name, sessionType);
            return sessionType;
        } else {
            context.metamodel().importType(QUARKUS_SESSION_OPERATIONS);
            if (element != null && isRecordReactiveRepository(element)) {
                context.setSessionGetter("SessionOperations.getStatelessSession()");
                return UNI_MUTINY_STATELESS_SESSION;
            } else {
                context.setSessionGetter("SessionOperations.getSession()");
                return UNI_MUTINY_SESSION;
            }
        }
    }

    private boolean isBlockingFavored(@Nullable TypeElement element, AnnotationMetaEntityContext context) {
        if (element != null) {
            if (usesQuarkusDataHibernate && isQuarkusDataRepository(element)) {
                return isQuarkusDataBlockingRepository(element);
            } else {
                for (var method : methodsIn(context.getAllMembers(element))) {
                    if (containsAnnotation(method, HQL, SQL, JD_QUERY, FIND, JD_FIND)) {
                        return !isUni(method.getReturnType());
                    }
                }
            }
        }
        return usesQuarkusOrm;
    }

    private String sessionGetterName(
            @Nullable ExecutableElement getter, @Nullable TypeElement element) {
        if (getter != null) {
            return getter.getSimpleName().toString();
        } else if (element != null && isRecordBlockingRepository(element)) {
            return "getStatelessSession";
        } else {
            return "getSession";
        }
    }

    private String sessionType(
            @Nullable ExecutableElement getter, @Nullable TypeElement element,
            AnnotationMetaEntityContext context) {
        if (getter != null) {
            return context.fullReturnType(getter);
        } else if (element != null && isRecordBlockingRepository(element)) {
            return HIB_STATELESS_SESSION;
        } else {
            return HIB_SESSION;
        }
    }

    private static boolean isUni(TypeMirror returnType) {
        if (returnType.getKind() == TypeKind.DECLARED) {
            final var declaredType = (DeclaredType) returnType;
            final var typeElement = (TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().contentEquals(UNI);
        }
        return false;
    }
}
