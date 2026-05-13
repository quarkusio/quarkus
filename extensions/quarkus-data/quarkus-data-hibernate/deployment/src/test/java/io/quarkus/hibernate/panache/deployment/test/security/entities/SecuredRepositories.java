package io.quarkus.hibernate.panache.deployment.test.security.entities;

import jakarta.inject.Singleton;

@Singleton
public final class SecuredRepositories {

    public final HibernateAnnotationFindEntity.MethodSecured hibernateFindMethodSecuredRepo;
    public final HibernateAnnotationFindEntity.ClassSecured hibernateFindClassSecuredRepo;
    public final HibernateAnnotationFindEntity.InnerPanacheRepository hibernateFindPanacheRepo;

    public final HibernateAnnotationHqlEntity.MethodSecured hibernateHqlMethodSecuredRepo;
    public final HibernateAnnotationHqlEntity.ClassSecured hibernateHqlClassSecuredRepo;
    public final HibernateAnnotationHqlEntity.InnerPanacheRepository hibernateHqlPanacheRepo;

    public final HibernateAnnotationSqlEntity.MethodSecured hibernateSqlMethodSecuredRepo;
    public final HibernateAnnotationSqlEntity.ClassSecured hibernateSqlClassSecuredRepo;
    public final HibernateAnnotationSqlEntity.InnerPanacheRepository hibernateSqlPanacheRepo;

    public final JakartaDataFindEntity.MethodSecured jdFindMethodSecuredRepo;
    public final JakartaDataFindEntity.ClassSecured jdFindClassSecuredRepo;
    public final JakartaDataFindEntity.InnerPanacheRepository jdFindPanacheRepo;

    public final JakartaDataQueryEntity.MethodSecured jdQueryMethodSecuredRepo;
    public final JakartaDataQueryEntity.ClassSecured jdQueryClassSecuredRepo;
    public final JakartaDataQueryEntity.InnerPanacheRepository jdQueryPanacheRepo;

    public final JakartaDataDeleteEntity.MethodSecured jdDeleteMethodSecuredRepo;
    public final JakartaDataDeleteEntity.ClassSecured jdDeleteClassSecuredRepo;
    public final JakartaDataDeleteEntity.InnerPanacheRepository jdDeletePanacheRepo;

    public final JakartaDataInsertEntity.MethodSecured jdInsertMethodSecuredRepo;
    public final JakartaDataInsertEntity.ClassSecured jdInsertClassSecuredRepo;
    public final JakartaDataInsertEntity.InnerPanacheRepository jdInsertPanacheRepo;

    public final JakartaDataUpdateEntity.MethodSecured jdUpdateMethodSecuredRepo;
    public final JakartaDataUpdateEntity.ClassSecured jdUpdateClassSecuredRepo;
    public final JakartaDataUpdateEntity.InnerPanacheRepository jdUpdatePanacheRepo;

    public final JakartaDataSaveEntity.MethodSecured jdSaveMethodSecuredRepo;
    public final JakartaDataSaveEntity.ClassSecured jdSaveClassSecuredRepo;
    public final JakartaDataSaveEntity.InnerPanacheRepository jdSavePanacheRepo;

    public final ManagedBlockingClassSecuredRepo managedBlockingClassSecuredRepo;
    public final StatelessBlockingMethodSecuredRepo statelessBlockingMethodSecuredRepo;
    public final StandaloneRepoEntity.InnerPanacheRepository standaloneRepoPanacheRepo;

    public final GenericRepoEntity.ChildRepo genericChildRepo;
    public final GenericRepoEntity.InnerPanacheRepository genericPanacheRepo;

    SecuredRepositories(
            HibernateAnnotationFindEntity.MethodSecured hibernateFindMethodSecuredRepo,
            HibernateAnnotationFindEntity.ClassSecured hibernateFindClassSecuredRepo,
            HibernateAnnotationFindEntity.InnerPanacheRepository hibernateFindPanacheRepo,
            HibernateAnnotationHqlEntity.MethodSecured hibernateHqlMethodSecuredRepo,
            HibernateAnnotationHqlEntity.ClassSecured hibernateHqlClassSecuredRepo,
            HibernateAnnotationHqlEntity.InnerPanacheRepository hibernateHqlPanacheRepo,
            HibernateAnnotationSqlEntity.MethodSecured hibernateSqlMethodSecuredRepo,
            HibernateAnnotationSqlEntity.ClassSecured hibernateSqlClassSecuredRepo,
            HibernateAnnotationSqlEntity.InnerPanacheRepository hibernateSqlPanacheRepo,
            JakartaDataFindEntity.MethodSecured jdFindMethodSecuredRepo,
            JakartaDataFindEntity.ClassSecured jdFindClassSecuredRepo,
            JakartaDataFindEntity.InnerPanacheRepository jdFindPanacheRepo,
            JakartaDataQueryEntity.MethodSecured jdQueryMethodSecuredRepo,
            JakartaDataQueryEntity.ClassSecured jdQueryClassSecuredRepo,
            JakartaDataQueryEntity.InnerPanacheRepository jdQueryPanacheRepo,
            JakartaDataDeleteEntity.MethodSecured jdDeleteMethodSecuredRepo,
            JakartaDataDeleteEntity.ClassSecured jdDeleteClassSecuredRepo,
            JakartaDataDeleteEntity.InnerPanacheRepository jdDeletePanacheRepo,
            JakartaDataInsertEntity.MethodSecured jdInsertMethodSecuredRepo,
            JakartaDataInsertEntity.ClassSecured jdInsertClassSecuredRepo,
            JakartaDataInsertEntity.InnerPanacheRepository jdInsertPanacheRepo,
            JakartaDataUpdateEntity.MethodSecured jdUpdateMethodSecuredRepo,
            JakartaDataUpdateEntity.ClassSecured jdUpdateClassSecuredRepo,
            JakartaDataUpdateEntity.InnerPanacheRepository jdUpdatePanacheRepo,
            JakartaDataSaveEntity.MethodSecured jdSaveMethodSecuredRepo,
            JakartaDataSaveEntity.ClassSecured jdSaveClassSecuredRepo,
            JakartaDataSaveEntity.InnerPanacheRepository jdSavePanacheRepo,
            ManagedBlockingClassSecuredRepo managedBlockingClassSecuredRepo,
            StatelessBlockingMethodSecuredRepo statelessBlockingMethodSecuredRepo,
            StandaloneRepoEntity.InnerPanacheRepository standaloneRepoPanacheRepo,
            GenericRepoEntity.ChildRepo genericChildRepo,
            GenericRepoEntity.InnerPanacheRepository genericPanacheRepo) {
        this.hibernateFindMethodSecuredRepo = hibernateFindMethodSecuredRepo;
        this.hibernateFindClassSecuredRepo = hibernateFindClassSecuredRepo;
        this.hibernateFindPanacheRepo = hibernateFindPanacheRepo;
        this.hibernateHqlMethodSecuredRepo = hibernateHqlMethodSecuredRepo;
        this.hibernateHqlClassSecuredRepo = hibernateHqlClassSecuredRepo;
        this.hibernateHqlPanacheRepo = hibernateHqlPanacheRepo;
        this.hibernateSqlMethodSecuredRepo = hibernateSqlMethodSecuredRepo;
        this.hibernateSqlClassSecuredRepo = hibernateSqlClassSecuredRepo;
        this.hibernateSqlPanacheRepo = hibernateSqlPanacheRepo;
        this.jdFindMethodSecuredRepo = jdFindMethodSecuredRepo;
        this.jdFindClassSecuredRepo = jdFindClassSecuredRepo;
        this.jdFindPanacheRepo = jdFindPanacheRepo;
        this.jdQueryMethodSecuredRepo = jdQueryMethodSecuredRepo;
        this.jdQueryClassSecuredRepo = jdQueryClassSecuredRepo;
        this.jdQueryPanacheRepo = jdQueryPanacheRepo;
        this.jdDeleteMethodSecuredRepo = jdDeleteMethodSecuredRepo;
        this.jdDeleteClassSecuredRepo = jdDeleteClassSecuredRepo;
        this.jdDeletePanacheRepo = jdDeletePanacheRepo;
        this.jdInsertMethodSecuredRepo = jdInsertMethodSecuredRepo;
        this.jdInsertClassSecuredRepo = jdInsertClassSecuredRepo;
        this.jdInsertPanacheRepo = jdInsertPanacheRepo;
        this.jdUpdateMethodSecuredRepo = jdUpdateMethodSecuredRepo;
        this.jdUpdateClassSecuredRepo = jdUpdateClassSecuredRepo;
        this.jdUpdatePanacheRepo = jdUpdatePanacheRepo;
        this.jdSaveMethodSecuredRepo = jdSaveMethodSecuredRepo;
        this.jdSaveClassSecuredRepo = jdSaveClassSecuredRepo;
        this.jdSavePanacheRepo = jdSavePanacheRepo;
        this.managedBlockingClassSecuredRepo = managedBlockingClassSecuredRepo;
        this.statelessBlockingMethodSecuredRepo = statelessBlockingMethodSecuredRepo;
        this.standaloneRepoPanacheRepo = standaloneRepoPanacheRepo;
        this.genericChildRepo = genericChildRepo;
        this.genericPanacheRepo = genericPanacheRepo;
    }
}
