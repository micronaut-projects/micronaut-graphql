package example1;

import example1.dto.Cat;
import example1.dto.Dog;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import io.micronaut.graphql.annotations.GraphQLTypeResolver;

@GraphQLTypeResolver(typeName = "Animal")
public class AnimalTypeResolver implements TypeResolver {

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment env) {
        Object javaObject = env.getObject();
        if (javaObject instanceof Cat) {
            return env.getSchema().getObjectType("Cat");
        } else if (javaObject instanceof Dog) {
            return env.getSchema().getObjectType("Dog");
        }
        throw new IllegalStateException();
    }
}
