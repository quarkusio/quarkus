package io.quarkus.hibernate.orm.functioncontributors;

import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

@ApplicationScoped
@PersistenceUnitExtension
public class CustomFunctionContributor implements FunctionContributor {
    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().register(
                "addHardcodedSuffix",
                new HardcodedSuffixFunction(
                        functionContributions.getTypeConfiguration(), "_some_suffix"
                )
        );
    }

    private static final class HardcodedSuffixFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
        private final String suffix;

        private HardcodedSuffixFunction(TypeConfiguration typeConfiguration, String suffix) {
            super("addHardcodedSuffix",
                    StandardArgumentsValidators.exactly(1),
                    StandardFunctionReturnTypeResolvers.invariant(
                            typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING)
                    ),
                    StandardFunctionArgumentTypeResolvers.impliedOrInvariant(typeConfiguration, STRING)
            );

            this.suffix = suffix;
        }

        @Override
        public void render(
                SqlAppender sqlAppender,
                List<? extends SqlAstNode> sqlAstArguments,
                ReturnableType<?> returnType,
                SqlAstTranslator<?> walker
        ) {
            sqlAppender.appendSql('(');
            walker.render(sqlAstArguments.get(0), SqlAstNodeRenderingMode.DEFAULT);
            sqlAppender.appendSql(" || '" + suffix + "')");
        }
    }
}
