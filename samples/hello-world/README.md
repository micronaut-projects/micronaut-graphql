# Micronaut GraphQL Hello World

Start sample application:

    ./gradlew :micronaut-graphql-samples-hello-world:run

Run GraphQL query:

    curl 'http://localhost:8080/graphql' -H 'content-type: application/json' --data-binary '{"query":"{\n  hello(name: \"Simba\")\n}\n"}'
