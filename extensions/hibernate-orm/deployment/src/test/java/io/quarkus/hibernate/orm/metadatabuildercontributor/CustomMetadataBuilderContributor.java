package io.quarkus.hibernate.orm.metadatabuildercontributor;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

public class CustomMetadataBuilderContributor implements MetadataBuilderContributor {
    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applySqlFunction(
                "addHardcodedSuffix",
                new HardcodedSuffixFunction("_some_suffix"));
    }

    private static final class HardcodedSuffixFunction implements SQLFunction {
        private final String suffix;

        private HardcodedSuffixFunction(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public boolean hasArguments() {
            return true;
        }

        @Override
        public boolean hasParenthesesIfNoArguments() {
            return false;
        }

        @Override
        public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
            return StringType.INSTANCE;
        }

        @Override
        public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
            return "(" + arguments.get(0) + " || '" + suffix + "')";
        }
    }
}
