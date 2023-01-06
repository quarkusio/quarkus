package io.quarkus.smallrye.graphql.runtime;

import java.util.Objects;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import io.vertx.ext.web.FileUpload;

public class SmallRyeGraphQLScalars {

    public static final GraphQLScalarType Upload = GraphQLScalarType.newScalar()
            .name("FileUpload")
            .description("A file part in a multipart request")
            .coercing(
                    new Coercing<FileUpload, Void>() {
                        @Override
                        public Void serialize(Object dataFetcherResult) {
                            throw new CoercingSerializeException("Upload is an input-only type");
                        }

                        @Override
                        public FileUpload parseValue(Object input) {
                            Objects.requireNonNull(input);
                            if (input instanceof FileUpload) {
                                return (FileUpload) input;
                            } else {
                                throw new CoercingParseValueException(
                                        "Expected type "
                                                + FileUpload.class.getName()
                                                + " but was "
                                                + input.getClass().getName());
                            }
                        }

                        @Override
                        public FileUpload parseLiteral(Object input) {
                            throw new CoercingParseLiteralException(
                                    "Must use variables to specify Upload values");
                        }
                    })
            .build();

}
