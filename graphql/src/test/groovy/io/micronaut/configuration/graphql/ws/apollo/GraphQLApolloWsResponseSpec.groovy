package io.micronaut.configuration.graphql.ws.apollo

import io.micronaut.core.type.Argument
import io.micronaut.context.BeanContext
import io.micronaut.serde.SerdeIntrospections
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(startApplication = false)
class GraphQLApolloWsResponseSpec extends Specification {

    @Inject
    BeanContext beanContext

    void "GraphQLApolloWsResponse is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(GraphQLApolloWsResponse))

        then:
        noExceptionThrown()
    }

    void "GraphQLApolloWsResponse is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(GraphQLApolloWsResponse))

        then:
        noExceptionThrown()
    }

    void "GraphQLApolloWsResponse.ServerType is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(GraphQLApolloWsResponse.ServerType))

        then:
        noExceptionThrown()
    }

    void "GraphQLApolloWsResponse.ServerType is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(GraphQLApolloWsResponse.ServerType))

        then:
        noExceptionThrown()
    }

}
