package example.graphql;

import example.domain.Author;
import example.domain.ToDo;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class ToDoResolver implements GraphQLResolver<ToDo> {

    @SuppressWarnings("unused")
    public CompletableFuture<Author> author(ToDo todo, DataFetchingEnvironment env) {
        DataLoader<String, Author> dataLoader = env.getDataLoader("author");

        return dataLoader.load(todo.getAuthorId());
    }

}
