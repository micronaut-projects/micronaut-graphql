package example.micronaut;

import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;

@Singleton
public class DbRepository {

    List<Book> findAllBooks() {
        return Collections.emptyList();
    }

}
