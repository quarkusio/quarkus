package org.jboss.shamrock.jpa;

import java.util.Objects;

import org.jboss.shamrock.deployment.ProcessorContext;

/**
 * This list of classes which any Hibernate ORM using application should register for reflective access on SubstrateVM.
 *
 * FIXME Find a reliable way to identify these and maintain the list accurate: the current list
 * is likely not complete as it was identified via a dumb "trial&error" strategy.
 *
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
final class HibernateReflectiveNeeds {

    private final ProcessorContext processorContext;

    private HibernateReflectiveNeeds(final ProcessorContext processorContext) {
        Objects.requireNonNull(processorContext);
        this.processorContext = processorContext;
    }

    public static void registerStaticReflectiveNeeds(final ProcessorContext processorContext) {
        Objects.requireNonNull(processorContext);
        new HibernateReflectiveNeeds(processorContext).registerAll();
    }

    private void registerAll() {
        //Various well known needs:
        simpleConstructor(org.hibernate.tuple.entity.PojoEntityTuplizer.class);
        simpleConstructor(org.hibernate.jpa.HibernatePersistenceProvider.class);
        simpleConstructor(org.hibernate.persister.entity.SingleTableEntityPersister.class);
        simpleConstructor(org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl.class);
        simpleConstructor(org.hibernate.id.enhanced.SequenceStyleGenerator.class);
        simpleConstructor(org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl.class);
        //FIXME following is not Hibernate specific?
        simpleConstructor("com.sun.xml.internal.stream.events.XMLEventFactoryImpl");
        //ANTLR tokens:
        simpleConstructor(org.hibernate.hql.internal.ast.HqlToken.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.Node.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.QueryNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SqlNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.FromClause.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.DotNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.IdentNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.FromElement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SelectClause.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SqlFragment.class);
        ///TODO ... several mode ANTLR tokens will be needed. Above will do for an hello world demo.

        //PostgreSQL specific (move to its own home?) FIXME
        simpleConstructor(org.hibernate.dialect.PostgreSQL95Dialect.class);
        simpleConstructor("org.postgresql.Driver");
    }

    /**
     * Register classes which we know will only need to be created via their no-arg constructor
     * @param clazz
     */
    private void simpleConstructor(final Class clazz) {
        simpleConstructor(clazz.getName());
    }

    private void simpleConstructor(final String clazzName) {
        processorContext.addReflectiveClass(false, false, clazzName);
    }

}
