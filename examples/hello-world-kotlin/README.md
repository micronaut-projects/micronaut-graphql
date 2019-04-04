# Micronaut GraphQL Hello World Kotlin example

Start the application:

    ./gradlew clean :graphql-example-hello-world-kotlin:run

Run GraphQL query:

    curl 'http://localhost:8080/graphql' -H 'content-type: application/json' --data-binary '{"query":"{\n  hello(name: \"Simba\")\n}\n"}'
