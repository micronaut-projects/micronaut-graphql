package example.micronaut;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class Author {

    private final String id;
    private final String firstName;
    private final String lastName;

    public Author(String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
