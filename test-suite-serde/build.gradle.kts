plugins {
    id("io.micronaut.build.internal.graphql.test-suite-serialization")
}

application {
    mainClass = "example.micronaut.Application"
}

dependencies {
    annotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
}
