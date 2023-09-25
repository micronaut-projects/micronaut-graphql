package example.micronaut;

import graphql.schema.DataFetcher;
import jakarta.inject.Singleton;

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
