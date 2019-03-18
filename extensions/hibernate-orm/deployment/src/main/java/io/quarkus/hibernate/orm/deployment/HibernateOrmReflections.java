/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.hibernate.orm.deployment;

import org.hibernate.type.EnumType;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

/**
 * This list of classes which any Hibernate ORM using application should register for reflective access on SubstrateVM.
 * <p>
 * FIXME Find a reliable way to identify these and maintain the list accurate: the current list
 * is likely not complete as it was identified via a dumb "trial&error" strategy.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HibernateOrmReflections {

    @BuildStep
    public void registerCoreReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Various well known needs:
        simpleConstructor(reflectiveClass, org.hibernate.tuple.entity.PojoEntityTuplizer.class);
        allConstructors(reflectiveClass, org.hibernate.tuple.component.PojoComponentTuplizer.class);
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
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, com.arjuna.ats.jta.UserTransaction.class.getName()));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, false, com.arjuna.ats.jta.TransactionManager.class.getName()));

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
