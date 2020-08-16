package example.domain;

/**
 * @author Alexey Zhokhov
 */
public class Author {

    private final String id;
    private final String username;

    public Author(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

}
