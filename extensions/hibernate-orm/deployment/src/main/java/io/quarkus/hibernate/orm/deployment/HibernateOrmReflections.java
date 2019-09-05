package io.quarkus.hibernate.orm.deployment;

import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.hibernate.type.EnumType;

import antlr.CommonToken;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * This list of classes which any Hibernate ORM using application should register for reflective access in native mode.
 * <p>
 * FIXME Find a reliable way to identify these and maintain the list accurate: the current list
 * is likely not complete as it was identified via a dumb "trial&error" strategy.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HibernateOrmReflections {

    @BuildStep
    public void registerCoreReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Eventlisteners need to be registered for reflection to allow creation via Array#newInstance ;
        // types need to be in synch with those declared in org.hibernate.event.spi.EventType
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.LoadEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.ResolveNaturalIdEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.InitializeCollectionEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.SaveOrUpdateEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PersistEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.MergeEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.DeleteEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.ReplicateEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.FlushEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.AutoFlushEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.DirtyCheckEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.FlushEntityEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.ClearEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.EvictEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.LockEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.RefreshEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PreLoadEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PreDeleteEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PreUpdateEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PreInsertEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PostLoadEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PostDeleteEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PostUpdateEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PostInsertEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PreCollectionRecreateEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PreCollectionRemoveEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PreCollectionUpdateEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PostCollectionRecreateEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PostCollectionRemoveEventListener[].class);
        simpleConstructor(reflectiveClass, org.hibernate.event.spi.PostCollectionUpdateEventListener[].class);

        //Various well known needs:
        simpleConstructor(reflectiveClass, org.hibernate.tuple.entity.PojoEntityTuplizer.class);
        simpleConstructor(reflectiveClass, org.hibernate.tuple.entity.DynamicMapEntityTuplizer.class);
        allConstructors(reflectiveClass, org.hibernate.tuple.component.PojoComponentTuplizer.class);
        simpleConstructor(reflectiveClass, org.hibernate.tuple.component.DynamicMapComponentTuplizer.class);
        allConstructors(reflectiveClass, org.hibernate.persister.collection.OneToManyPersister.class);
        allConstructors(reflectiveClass, org.hibernate.persister.collection.BasicCollectionPersister.class);
        simpleConstructor(reflectiveClass, org.hibernate.persister.entity.SingleTableEntityPersister.class);
        allConstructors(reflectiveClass, org.hibernate.persister.entity.JoinedSubclassEntityPersister.class);
        allConstructors(reflectiveClass, org.hibernate.persister.entity.UnionSubclassEntityPersister.class);
        simpleConstructor(reflectiveClass,
                org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl.class);
        simpleConstructor(reflectiveClass, org.hibernate.id.enhanced.SequenceStyleGenerator.class);
        simpleConstructor(reflectiveClass, org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl.class);
        simpleConstructor(reflectiveClass,
                org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl.class);
        simpleConstructor(reflectiveClass, EnumType.class);
        simpleConstructor(reflectiveClass, MultipleLinesSqlCommandExtractor.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.HqlToken.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.Node.class);

        //ANTLR tokens:
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.SelectClause.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.HqlSqlWalkerNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.MethodNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.AbstractStatement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.EntityJoinFromElement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.UnaryLogicOperatorNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.NullNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.IntoClause.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.AbstractRestrictableStatement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.UpdateStatement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.SelectExpressionImpl.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.CastFunctionNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.MapKeyEntityFromElement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.DeleteStatement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.SqlNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.SearchedCaseNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.FromElement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.JavaConstantNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.SelectExpressionList.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.SqlFragment.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.MapKeyNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.ImpliedFromElement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.IsNotNullLogicOperatorNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.InsertStatement.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.UnaryArithmeticNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.CollectionFunction.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.BinaryLogicOperatorNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.CountNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.IsNullLogicOperatorNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.IdentNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.ComponentJoin.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.ParameterNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.AbstractSelectExpression.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.MapEntryNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.MapValueNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.InLogicOperatorNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.IndexNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.AbstractNullnessCheckNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.DotNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.ResultVariableRefNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.BetweenOperatorNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.AggregateNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.QueryNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.BooleanLiteralNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.SimpleCaseNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.AbstractMapComponentNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.FromReferenceNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.OrderByClause.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.FromClause.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.ConstructorNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.LiteralNode.class);
        simpleConstructor(reflectiveClass, org.hibernate.hql.internal.ast.tree.BinaryArithmeticOperatorNode.class);
        simpleConstructor(reflectiveClass, CommonToken.class);

        // Support for @OrderBy
        simpleConstructor(reflectiveClass, org.hibernate.sql.ordering.antlr.NodeSupport.class);
        simpleConstructor(reflectiveClass, org.hibernate.sql.ordering.antlr.OrderByFragment.class);
        simpleConstructor(reflectiveClass, org.hibernate.sql.ordering.antlr.SortSpecification.class);
        simpleConstructor(reflectiveClass, org.hibernate.sql.ordering.antlr.OrderingSpecification.class);
        simpleConstructor(reflectiveClass, org.hibernate.sql.ordering.antlr.CollationSpecification.class);
        simpleConstructor(reflectiveClass, org.hibernate.sql.ordering.antlr.SortKey.class);

        //The CoreMessageLogger is sometimes looked up without it necessarily being a field, so we're
        //not processing it the same way as other Logger lookups.
        simpleConstructor(reflectiveClass, "org.hibernate.internal.CoreMessageLogger_$logger");
    }

    private void allConstructors(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, final Class<?> clazz) {
        //FIXME simpleConstructor is not optimized yet to only enlist the no-arg constructor
        simpleConstructor(reflectiveClass, clazz);
    }

    /**
     * Register classes which we know will only need to be created via their no-arg constructor
     */
    private void simpleConstructor(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, final Class<?> clazz) {
        simpleConstructor(reflectiveClass, clazz.getName());
    }

    private void simpleConstructor(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, final String clazzName) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, clazzName));
    }

}
