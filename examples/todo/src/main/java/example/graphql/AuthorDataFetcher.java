package example.graphql;

import example.domain.Author;
import example.domain.ToDo;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class AuthorDataFetcher implements DataFetcher<CompletionStage<Author>> {

    @Override
    public CompletionStage<Author> get(DataFetchingEnvironment environment) throws Exception {
        ToDo toDo = environment.getSource();
        DataLoader<String, Author> authorDataLoader = environment.getDataLoader("author");
        return authorDataLoader.load(toDo.getAuthorId());
    }

}
