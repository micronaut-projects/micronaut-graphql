package io.micronaut.configuration.graphql.ws.apollo

import io.micronaut.context.BeanContext
import io.micronaut.core.type.Argument
import io.micronaut.serde.SerdeIntrospections
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(startApplication = false)
class GraphQLApolloWsRequestSpec extends Specification {

    @Inject
    BeanContext beanContext

    void "GraphQLApolloWsRequest is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(GraphQLApolloWsRequest))

        then:
        noExceptionThrown()
    }

    void "GraphQLApolloWsRequest is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(GraphQLApolloWsRequest))

        then:
        noExceptionThrown()
    }

    void "io.micronaut.configuration.graphql.ws.apollo.GraphQLApolloWsRequest.ClientType is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(GraphQLApolloWsRequest.ClientType))

        then:
        noExceptionThrown()
    }

    void "io.micronaut.configuration.graphql.ws.apollo.GraphQLApolloWsRequest.ClientType is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(GraphQLApolloWsRequest.ClientType))

        then:
        noExceptionThrown()
    }
}
