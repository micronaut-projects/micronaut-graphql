package example.micronaut;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;

@Requires(property= "spec.name", value = "BookTest")
@Singleton
public class DbRepository {

    List<Book> findAllBooks() {
        return Collections.emptyList();
    }

}
