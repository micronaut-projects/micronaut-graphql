# Micronaut GraphQL Hello World Java Tools

Start sample application:

    ./gradlew :micronaut-graphql-samples-hello-world-java-tools:run

Run GraphQL query:

    curl 'http://localhost:8080/graphql' -H 'content-type: application/json' --data-binary '{"query":"{\n  hello(name: \"Simba\")\n}\n"}'
