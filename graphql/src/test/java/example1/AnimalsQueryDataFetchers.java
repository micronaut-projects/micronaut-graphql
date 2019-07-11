package example1;

import example1.dto.Cat;
import graphql.schema.DataFetchingEnvironment;
import io.micronaut.graphql.annotations.GraphQLDataFetcher;

@GraphQLDataFetcher(typeName = "Cat")
public class AnimalsQueryDataFetchers {

    public String prefixName(String prefix, Cat cat) {
        return prefix + cat.getName();
    }

    @GraphQLDataFetcher(typeName = "Cat", fieldName = "internalClassName")
    public String overrideName(DataFetchingEnvironment environment) {
        return environment.getSource().getClass().getSimpleName();
    }

    // Override type name
    @GraphQLDataFetcher(typeName = "Dog", fieldName = "internalClassName")
    public String overrideNameForDog(DataFetchingEnvironment environment) {
        return environment.getSource().getClass().getSimpleName();
    }

}
