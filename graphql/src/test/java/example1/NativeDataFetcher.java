package example1;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.micronaut.graphql.annotations.GraphQLQueryDataFetcher;

@GraphQLQueryDataFetcher(fieldName = "weather")
public class NativeDataFetcher implements DataFetcher<String> {
    @Override
    public String get(DataFetchingEnvironment environment) {
        return "Sunny";
    }
}
