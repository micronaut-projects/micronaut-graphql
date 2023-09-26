package example.micronaut;

import graphql.schema.DataFetcher;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

@Requires(property= "spec.name", value = "BookTest")
@Singleton
public class GraphQLDataFetchers {

    private final DbRepository dbRepository;

    public GraphQLDataFetchers(DbRepository dbRepository) {
        this.dbRepository = dbRepository;
    }

    public DataFetcher<Iterable<Book>> getBookDataFetcher() {
        return dataFetchingEnvironment -> dbRepository.findAllBooks();
    }
}
