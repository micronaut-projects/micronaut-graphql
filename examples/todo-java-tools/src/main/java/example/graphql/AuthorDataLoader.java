package example.graphql;

import example.domain.Author;
import example.repository.AuthorRepository;
import org.dataloader.MappedBatchLoader;

import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class AuthorDataLoader implements MappedBatchLoader<String, Author> {

    private final AuthorRepository authorRepository;

    public AuthorDataLoader(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Override
    public CompletionStage<Map<String, Author>> load(Set<String> keys) {
        return CompletableFuture.completedFuture(
                authorRepository
                        .findAllById(keys)
                        .stream()
                        .collect(toMap(Author::getId, Function.identity()))
        );
    }

}
