package example.micronaut;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class Book {

    private final String id;
    private final String name;
    private final int pageCount;
    private final Author author;

    public Book(String id, String name, int pageCount, Author author) {
        this.id = id;
        this.name = name;
        this.pageCount = pageCount;
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPageCount() {
        return pageCount;
    }

    public Author getAuthor() {
        return author;
    }
}
