package io.quarkus.hibernate.panache.deployment.test.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Permission;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.data.Order;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.hibernate.panache.deployment.test.security.entities.GenericRepoEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.HibernateAnnotationFindEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.HibernateAnnotationHqlEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.HibernateAnnotationSqlEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.JakartaDataDeleteEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.JakartaDataFindEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.JakartaDataInsertEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.JakartaDataQueryEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.JakartaDataSaveEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.JakartaDataUpdateEntity;
import io.quarkus.hibernate.panache.deployment.test.security.entities.SecuredRepositories;
import io.quarkus.hibernate.panache.deployment.test.security.entities.StandaloneRepoEntity;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

@ActivateRequestContext
class PanacheNextSecuredRepositoryTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addPackage(HibernateAnnotationFindEntity.class.getPackage()))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-security-deployment", Version.getVersion())));

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @Inject
    SecuredRepositories repos;

    @Test
    void testHibernateFind() {
        clearIdentity();
        HibernateAnnotationFindEntity entity = new HibernateAnnotationFindEntity();
        entity.name = "test";
        createEntity(entity, repos.hibernateFindPanacheRepo);

        try {
            assertAuthenticationRequired(repos.hibernateFindMethodSecuredRepo::findByName, "test");
            assertAuthenticationRequired(repos.hibernateFindClassSecuredRepo::findByName, "test");

            setIdentity("user");
            assertActionAllowed(() -> assertThat(repos.hibernateFindMethodSecuredRepo.findByName("test")).hasSize(1));
        } finally {
            cleanUp(repos.hibernateFindPanacheRepo);
        }
    }

    @Test
    void testHibernateHql() {
        clearIdentity();
        HibernateAnnotationHqlEntity entity = new HibernateAnnotationHqlEntity();
        entity.name = "test";
        createEntity(entity, repos.hibernateHqlPanacheRepo);

        try {
            assertAuthenticationRequired(repos.hibernateHqlMethodSecuredRepo::findByName, "test");
            assertAuthenticationRequired(repos.hibernateHqlClassSecuredRepo::findByName, "test");

            setIdentity("user");
            assertActionAllowed(() -> assertThat(repos.hibernateHqlMethodSecuredRepo.findByName("test")).hasSize(1));
        } finally {
            cleanUp(repos.hibernateHqlPanacheRepo);
        }
    }

    @Test
    void testHibernateSql() {
        clearIdentity();
        HibernateAnnotationSqlEntity entity = new HibernateAnnotationSqlEntity();
        entity.name = "test";
        createEntity(entity, repos.hibernateSqlPanacheRepo);

        try {
            assertAuthenticationRequired(repos.hibernateSqlMethodSecuredRepo::findByName, "test");
            assertAuthenticationRequired(repos.hibernateSqlClassSecuredRepo::findByName, "test");

            setIdentity("user");
            assertActionAllowed(() -> assertThat(repos.hibernateSqlMethodSecuredRepo.findByName("test")).hasSize(1));
        } finally {
            cleanUp(repos.hibernateSqlPanacheRepo);
        }
    }

    @Test
    void testJakartaDataFind() {
        clearIdentity();
        JakartaDataFindEntity entity = new JakartaDataFindEntity();
        entity.name = "test";
        createEntity(entity, repos.jdFindPanacheRepo);

        try {
            assertAuthenticationRequired(repos.jdFindMethodSecuredRepo::findByName, "test");
            assertAuthenticationRequired(repos.jdFindClassSecuredRepo::findByName, "test");

            setIdentity("user");
            assertActionAllowed(() -> assertThat(repos.jdFindMethodSecuredRepo.findByName("test")).hasSize(1));
        } finally {
            cleanUp(repos.jdFindPanacheRepo);
        }
    }

    @Test
    void testJakartaDataQuery() {
        clearIdentity();
        JakartaDataQueryEntity entity = new JakartaDataQueryEntity();
        entity.name = "test";
        createEntity(entity, repos.jdQueryPanacheRepo);

        try {
            assertAuthenticationRequired(repos.jdQueryMethodSecuredRepo::findByName, "test");
            assertAuthenticationRequired(repos.jdQueryClassSecuredRepo::findByName, "test");

            setIdentity("user");
            assertActionAllowed(() -> assertThat(repos.jdQueryMethodSecuredRepo.findByName("test")).hasSize(1));
        } finally {
            cleanUp(repos.jdQueryPanacheRepo);
        }
    }

    @RunOnVertxContext
    @Test
    void testJakartaDataDelete(UniAsserter asserter) {
        clearIdentity();

        JakartaDataDeleteEntity entity = new JakartaDataDeleteEntity();
        entity.name = "test";
        asserter.execute(() -> createEntityReactive(entity));

        asserter.assertFailedWith(this::deleteEntityReactively_MethodSecurity, UnauthorizedException.class);
        asserter.assertFailedWith(this::deleteEntityReactively_ClassSecurity, UnauthorizedException.class);

        asserter.assertThat(this::deleteEntityReactively_MethodSecurity_WithUser, c -> assertThat(c).isEqualTo(1));
        asserter.assertThat(this::countEntityReactive, c -> assertThat(c).isEqualTo(0));
    }

    @Test
    void testJakartaDataInsert() {
        clearIdentity();
        JakartaDataInsertEntity entity = new JakartaDataInsertEntity();
        entity.name = "test";

        try {
            assertAuthenticationRequired(repos.jdInsertMethodSecuredRepo::myInsert, entity);
            assertAuthenticationRequired(repos.jdInsertClassSecuredRepo::myInsert, entity);

            setIdentity("user");
            assertThat(entity.id).isNull();
            assertActionAllowed(() -> repos.jdInsertMethodSecuredRepo.myInsert(entity));
            assertThat(entity.id).isNotNull();
            assertThat(findEntity(entity.id, repos.jdInsertPanacheRepo).name).isEqualTo("test");
        } finally {
            cleanUp(repos.jdInsertPanacheRepo);
        }
    }

    @Test
    void testJakartaDataUpdate() {
        clearIdentity();
        JakartaDataUpdateEntity entity = new JakartaDataUpdateEntity();
        entity.name = "original";
        createEntity(entity, repos.jdUpdatePanacheRepo);

        try {
            assertAuthenticationRequired(repos.jdUpdateMethodSecuredRepo::myUpdate, entity);
            assertAuthenticationRequired(repos.jdUpdateClassSecuredRepo::myUpdate, entity);

            setIdentity("user");
            entity.name = "updated";
            assertActionAllowed(() -> repos.jdUpdateMethodSecuredRepo.myUpdate(entity));
            assertThat(findEntity(entity.id, repos.jdUpdatePanacheRepo).name).isEqualTo("updated");
        } finally {
            cleanUp(repos.jdUpdatePanacheRepo);
        }
    }

    @Test
    void testJakartaDataSave() {
        clearIdentity();
        JakartaDataSaveEntity entity = new JakartaDataSaveEntity();
        entity.name = "test";

        try {
            assertAuthenticationRequired(repos.jdSaveMethodSecuredRepo::mySave, entity);
            assertAuthenticationRequired(repos.jdSaveClassSecuredRepo::mySave, entity);

            setIdentity("user");
            assertActionAllowed(() -> repos.jdSaveMethodSecuredRepo.mySave(entity));
            assertThat(findEntity(entity.id, repos.jdSavePanacheRepo).name).isEqualTo("test");
        } finally {
            cleanUp(repos.jdSavePanacheRepo);
        }
    }

    @Test
    void testManagedBlockingClassSecured() {
        clearIdentity();
        StandaloneRepoEntity entity = new StandaloneRepoEntity();
        entity.name = "test";
        createEntity(entity, repos.standaloneRepoPanacheRepo);

        try {
            assertAuthenticationRequired(repos.managedBlockingClassSecuredRepo::findByName, "test");

            setIdentity("user");
            assertAuthorizationRequired(repos.managedBlockingClassSecuredRepo::findByName, "test");

            setIdentity("admin");
            assertActionAllowed(() -> assertThat(repos.managedBlockingClassSecuredRepo.findByName("test")).hasSize(1));
        } finally {
            cleanUp(repos.standaloneRepoPanacheRepo);
        }
    }

    @Test
    void testStatelessBlockingMethodSecured() {
        clearIdentity();
        StandaloneRepoEntity entity = new StandaloneRepoEntity();
        entity.name = "test";
        createEntity(entity, repos.standaloneRepoPanacheRepo);

        try {
            assertAuthenticationRequired(repos.statelessBlockingMethodSecuredRepo::findByName, "test");
            assertAuthenticationRequired(repos.statelessBlockingMethodSecuredRepo::hqlByName, "test");
            assertActionAllowed(
                    () -> assertThat(repos.statelessBlockingMethodSecuredRepo.unsecuredFindByName("test")).hasSize(1));

            // @RolesAllowed requires 'admin' => expect ForbiddenException
            setIdentity("user");
            assertAuthorizationRequired(repos.statelessBlockingMethodSecuredRepo::findByName, "test");
            assertActionAllowed(
                    () -> assertThat(repos.statelessBlockingMethodSecuredRepo.unsecuredFindByName("test")).hasSize(1));
            assertAuthorizationRequired(repos.statelessBlockingMethodSecuredRepo::hqlByName, "test");

            setIdentity("admin");
            assertActionAllowed(() -> assertThat(repos.statelessBlockingMethodSecuredRepo.findByName("test")).hasSize(1));
            // @PermissionsAllowed requires 'admin' && 'root' => expect ForbiddenException
            assertAuthorizationRequired(repos.statelessBlockingMethodSecuredRepo::hqlByName, "test");

            setIdentity("admin", "root");
            assertActionAllowed(() -> assertThat(repos.statelessBlockingMethodSecuredRepo.hqlByName("test")).hasSize(1));
        } finally {
            cleanUp(repos.standaloneRepoPanacheRepo);
        }
    }

    @Test
    void testGenericParentRepo() {
        clearIdentity();
        GenericRepoEntity entity = new GenericRepoEntity();
        entity.name = "test";
        createEntity(entity, repos.genericPanacheRepo);

        try {
            // generics works -> we test it as we document it
            assertActionAllowed(() -> assertThat(repos.genericChildRepo.findAll(Order.by())).hasSize(1));

            // we document that using the explicit type will be secured, hence test it
            assertAuthenticationRequired(ignored -> repos.genericChildRepo.securedFindAll(Order.by()), null);
            setIdentity("user");
            assertActionAllowed(() -> assertThat(repos.genericChildRepo.securedFindAll(Order.by())).hasSize(1));
        } finally {
            cleanUp(repos.genericPanacheRepo);
        }
    }

    @WithTransaction
    Uni<Integer> deleteEntityReactively_ClassSecurity() {
        return repos.jdDeleteClassSecuredRepo.deleteByName("test");
    }

    @WithTransaction
    Uni<Integer> deleteEntityReactively_MethodSecurity() {
        return repos.jdDeleteMethodSecuredRepo.deleteByName("test");
    }

    @WithTransaction
    Uni<Integer> deleteEntityReactively_MethodSecurity_WithUser() {
        return Uni.createFrom().deferred(() -> {
            setIdentity("user");
            return repos.jdDeleteMethodSecuredRepo.deleteByName("test");
        });
    }

    @WithTransaction
    Uni<Void> createEntityReactive(JakartaDataDeleteEntity entity) {
        return repos.jdDeletePanacheRepo.persist(entity);
    }

    @WithTransaction
    Uni<Long> countEntityReactive() {
        return Uni.createFrom().deferred(() -> {
            setIdentity("user");
            return repos.jdDeletePanacheRepo.count();
        });
    }

    @Transactional
    <T> T findEntity(Long id, PanacheRepository<T> panacheRepository) {
        return panacheRepository.findById(id);
    }

    @Transactional
    <T> void cleanUp(PanacheRepository<T> panacheRepository) {
        clearIdentity();
        assertActionAllowed(panacheRepository::deleteAll);
        assertThat(panacheRepository.count()).isEqualTo(0);
    }

    @Transactional
    <T> void assertAuthenticationRequired(Consumer<T> consumer, T entity) {
        assertThatThrownBy(() -> consumer.accept(entity)).isInstanceOf(UnauthorizedException.class);
    }

    @Transactional
    <T> void assertAuthorizationRequired(Consumer<T> consumer, T entity) {
        assertThatThrownBy(() -> consumer.accept(entity)).isInstanceOf(ForbiddenException.class);
    }

    @Transactional
    void assertActionAllowed(Runnable runnable) {
        assertThatNoException().isThrownBy(runnable::run);
    }

    <T> void createEntity(T entity, PanacheRepository<T> panacheRepository) {
        assertActionAllowed(() -> panacheRepository.persist(entity));
    }

    private void clearIdentity() {
        currentIdentityAssociation.setIdentity((SecurityIdentity) null);
    }

    private void setIdentity(String roleName, String... otherPermissions) {
        currentIdentityAssociation.setIdentity(new SecurityIdentity() {
            @Override
            public Principal getPrincipal() {
                return () -> roleName;
            }

            @Override
            public boolean isAnonymous() {
                return false;
            }

            @Override
            public Set<String> getRoles() {
                return Set.of(roleName);
            }

            @Override
            public boolean hasRole(String role) {
                return getRoles().contains(role);
            }

            @Override
            public Set<Permission> getPermissions() {
                var perms = new HashSet<Permission>();
                perms.add(new StringPermission(roleName));
                for (String otherPermission : otherPermissions) {
                    perms.add(new StringPermission(otherPermission));
                }
                return perms;
            }

            @Override
            public <T extends Credential> T getCredential(Class<T> credentialType) {
                return null;
            }

            @Override
            public Set<Credential> getCredentials() {
                return Set.of();
            }

            @Override
            public <T> T getAttribute(String name) {
                return null;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Map.of();
            }

            @Override
            public Uni<Boolean> checkPermission(Permission required) {
                boolean grantAccess = getPermissions().stream().anyMatch(possessed -> possessed.implies(required));
                return Uni.createFrom().item(grantAccess);
            }
        });
    }
}
