package org.jboss.shamrock.jpa;

import java.util.Objects;

import org.hibernate.tuple.component.PojoComponentTuplizer;
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
        allConstructors(org.hibernate.tuple.component.PojoComponentTuplizer.class);
        simpleConstructor(org.hibernate.persister.entity.SingleTableEntityPersister.class);
        simpleConstructor(org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl.class);
        simpleConstructor(org.hibernate.id.enhanced.SequenceStyleGenerator.class);
        simpleConstructor(org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl.class);
        simpleConstructor(org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl.class);
        processorContext.addReflectiveClass(true, false, com.arjuna.ats.jta.UserTransaction.class.getName());
        processorContext.addReflectiveClass(true, false, com.arjuna.ats.jta.TransactionManager.class.getName());

        //FIXME following is not Hibernate specific?
        simpleConstructor("com.sun.xml.internal.stream.events.XMLEventFactoryImpl");
        simpleConstructor(org.hibernate.hql.internal.ast.HqlToken.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.Node.class);


        //ANTLR tokens:
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SelectClause.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.HqlSqlWalkerNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.MethodNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.AbstractStatement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.EntityJoinFromElement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.UnaryLogicOperatorNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.NullNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.IntoClause.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.AbstractRestrictableStatement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.UpdateStatement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SelectExpressionImpl.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.CastFunctionNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.MapKeyEntityFromElement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.DeleteStatement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SqlNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SearchedCaseNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.FromElement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.JavaConstantNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SelectExpressionList.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SqlFragment.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.MapKeyNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.ImpliedFromElement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.IsNotNullLogicOperatorNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.InsertStatement.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.UnaryArithmeticNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.CollectionFunction.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.BinaryLogicOperatorNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.CountNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.IsNullLogicOperatorNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.IdentNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.ComponentJoin.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.ParameterNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.AbstractSelectExpression.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.MapEntryNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.MapValueNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.InLogicOperatorNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.IndexNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.AbstractNullnessCheckNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.DotNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.ResultVariableRefNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.BetweenOperatorNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.AggregateNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.QueryNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.BooleanLiteralNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.SimpleCaseNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.AbstractMapComponentNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.FromReferenceNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.OrderByClause.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.FromClause.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.ConstructorNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.LiteralNode.class);
        simpleConstructor(org.hibernate.hql.internal.ast.tree.BinaryArithmeticOperatorNode.class);

        //PostgreSQL specific (move to its own home?) FIXME
        simpleConstructor(org.hibernate.dialect.PostgreSQL95Dialect.class);
        simpleConstructor("org.postgresql.Driver");
    }

    private void allConstructors(final Class clazz) {
        //FIXME simpleConstructor is not optimized yet to only enlist the no-arg constructor
        simpleConstructor(clazz);
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
