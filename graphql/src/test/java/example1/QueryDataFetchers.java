package example1;

import example1.dto.Animal;
import example1.dto.Cat;
import example1.dto.Dog;
import io.micronaut.graphql.annotations.GraphQLFieldName;
import io.micronaut.graphql.annotations.GraphQLQueryDataFetcher;

@GraphQLQueryDataFetcher
public class QueryDataFetchers {

    public String hello() {
        return "Hello world";
    }

    @GraphQLFieldName("randomNumber")
    public Integer notRandomNumber() {
        return 42;
    }

    public Animal favoriteAnimal() {
        return cat();
    }

    public Animal cat() {
        Cat cat = new Cat();
        cat.setName("Snow");
        return cat;
    }

    public Animal dog() {
        Dog dog = new Dog();
        dog.setName("Ant");
        return dog;
    }

}
