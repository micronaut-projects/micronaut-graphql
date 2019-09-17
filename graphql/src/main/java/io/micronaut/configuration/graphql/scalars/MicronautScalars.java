package io.micronaut.configuration.graphql.scalars;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import io.micronaut.http.multipart.CompletedPart;

/**
 * This is the API entry point for Micronaut specific scalars.
 */
public class MicronautScalars {

    /**
     * TODO.
     */
    public static GraphQLScalarType Upload = GraphQLScalarType.newScalar()
            .name("Upload")
            .description("An Upload Scalar")
            .coercing(new Coercing<CompletedPart, Void>() {

                @Override
                public Void serialize(Object dataFetcherResult) throws CoercingSerializeException {
                    throw new CoercingSerializeException("Upload is an input-only type");
                }

                @Override
                public CompletedPart parseValue(Object input) throws CoercingParseValueException {
                    if (input instanceof CompletedPart) {
                        return (CompletedPart) input;
                    } else if (input == null) {
                        return null;
                    } else {
                        throw new CoercingParseValueException(String.format("Expected type %s but was %s",
                                CompletedPart.class.getName(), input.getClass().getName()));
                    }
                }

                @Override
                public CompletedPart parseLiteral(Object input) throws CoercingParseLiteralException {
                    throw new CoercingParseLiteralException("Must use variables to specify Upload values");
                }
            })
            .build();
}
