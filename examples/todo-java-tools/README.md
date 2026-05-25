# Micronaut GraphQL To-Do Java Tools example

Demonstrates the usage of [graphql-java-tools](https://www.graphql-java-kickstart.com/tools/) which provides APIs to support more easily build a GraphQL endpoint based off your schema and domain objects.

This example also shows how to use a GraphQL `DataLoader` to batch nested author lookups for the `ToDo.author` field.

## Running

Start the application:

    ./gradlew clean :graphql-example-todo-java-tools:run
    
Open the embedded [Graph<i>i</i>QL](http://localhost:8080/graphiql) IDE to interact with the GraphQL To-Do API.

## DataLoader flow

The `todo-java-tools` example wires the `author` field through a request-scoped `DataLoaderRegistry` (and loader cache):

* `DataLoaderRegistryFactory` creates a new `DataLoaderRegistry` for each request and registers the `author` data loader.
* `AuthorDataLoader` implements `MappedBatchLoader<String, Author>` and batches author lookups through `AuthorRepository`.
* `ToDoResolver` manually retrieves the loader from `DataFetchingEnvironment` and calls `load(todo.getAuthorId())`.

The `@RequestScope` on the `dataLoaderRegistry()` bean method inside `DataLoaderRegistryFactory` is important. It ensures every GraphQL request gets a fresh loader registry and cache.

GraphQL does not invoke the data loader automatically. The resolver must call it for the field that should be batched.

To exercise the data loader, run a query that resolves authors for multiple todos:

```graphql
query {
  toDos {
    title
    author {
      username
    }
  }
}
```
