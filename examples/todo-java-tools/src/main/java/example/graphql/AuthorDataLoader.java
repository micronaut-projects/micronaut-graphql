package example.graphql;

import example.domain.Author;
import example.repository.AuthorRepository;
import io.micronaut.scheduling.TaskExecutors;
import org.dataloader.MappedBatchLoader;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class AuthorDataLoader implements MappedBatchLoader<String, Author> {

    private final AuthorRepository authorRepository;
    private final ExecutorService executor;

    public AuthorDataLoader(AuthorRepository authorRepository,
                            @Named(TaskExecutors.IO) ExecutorService executor) {
        this.authorRepository = authorRepository;
        this.executor = executor;
    }

    @Override
    public CompletionStage<Map<String, Author>> load(Set<String> keys) {
        return CompletableFuture.supplyAsync(() ->
                authorRepository
                        .findAllById(keys)
                        .stream()
                        .collect(toMap(Author::getId, Function.identity())), executor);
    }

}
