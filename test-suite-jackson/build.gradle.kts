plugins {
    id("io.micronaut.build.internal.graphql.test-suite-serialization")
}

application {
    mainClass = "example.micronaut.Application"
}

dependencies {
    testImplementation(mn.micronaut.jackson.databind)
}
