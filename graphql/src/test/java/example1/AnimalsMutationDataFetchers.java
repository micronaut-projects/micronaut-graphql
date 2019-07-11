package example1;

import example1.dto.Dog;
import io.micronaut.graphql.annotations.GraphQLMutationDataFetcher;

@GraphQLMutationDataFetcher
public class AnimalsMutationDataFetchers {

    public Dog createDog(String name) {
        Dog dog = new Dog();
        dog.setName(name);
        return dog;
    }

}
