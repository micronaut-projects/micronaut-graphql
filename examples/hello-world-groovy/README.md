# Micronaut GraphQL Hello World Groovy example

Start the application:

    ./gradlew clean :graphql-example-hello-world-groovy:run

Run GraphQL query:

    curl 'http://localhost:8080/graphql' -H 'content-type: application/json' --data-binary '{"query":"{\n  hello(name: \"Simba\")\n}\n"}'
