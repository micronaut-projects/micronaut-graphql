package example.micronaut;

import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class DbRepository {

    private static final List<Book> books = Arrays.asList(
        new Book("book-1", "Harry Potter and the Philosopher's Stone", 223, new Author("author-1", "Joanne", "Rowling")),
        new Book("book-2", "Moby Dick", 635, new Author("author-2", "Herman", "Melville")),
        new Book("book-3", "Interview with the vampire", 371, new Author("author-3", "Anne", "Rice"))
    );

    List<Book> findAllBooks() {
        return books;
    }

    List<Author> findAllAuthors() {
        return books.stream()
            .map(Book::getAuthor)
            .collect(Collectors.toList());
    }
}
