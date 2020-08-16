package example.repository;

import example.domain.Author;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class AuthorRepository {

    private final Map<String, Author> authors = new HashMap<>();

    public List<Author> findAllById(Collection<String> ids) {
        return authors.values()
                .stream()
                .filter(it -> ids.contains(it.getId()))
                .collect(Collectors.toList());
    }

    public Author findOrCreate(String username) {
        if (!authors.containsKey(username)) {
            authors.put(username, new Author(UUID.randomUUID().toString(), username));
        }

        return authors.get(username);
    }

}
