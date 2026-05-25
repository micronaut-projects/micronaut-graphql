package io.micronaut.configuration.graphql.ws

import io.micronaut.configuration.graphql.GraphQLJsonSerializer
import io.micronaut.context.BeanContext
import io.micronaut.core.type.Argument
import io.micronaut.serde.SerdeIntrospections
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(environments = "websocket")
class MessageSpec extends Specification {
    @Inject
    BeanContext beanContext

    @Inject
    GraphQLJsonSerializer graphQLJsonSerializer

    void "ConnectionInitMessage is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(ConnectionInitMessage))

        then:
        noExceptionThrown()
    }

    void "ConnectionInitMessage is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(ConnectionInitMessage))

        then:
        noExceptionThrown()
    }

    void "PingMessage is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(PingMessage))

        then:
        noExceptionThrown()
    }

    void "PingMessage is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(PingMessage))

        then:
        noExceptionThrown()
    }

    void "PongMessage is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(PongMessage))

        then:
        noExceptionThrown()
    }

    void "PongMessage is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(PongMessage))

        then:
        noExceptionThrown()
    }

    void "NextMessage is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(NextMessage))

        then:
        noExceptionThrown()
    }

    void "NextMessage is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(NextMessage))

        then:
        noExceptionThrown()
    }

    void "SubscribeMessage is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(SubscribeMessage))

        then:
        noExceptionThrown()
    }

    void "SubscribeMessage is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(SubscribeMessage))

        then:
        noExceptionThrown()
    }

    void "SubscribePayload is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(SubscribeMessage.SubscribePayload))

        then:
        noExceptionThrown()
    }

    void "SubscribePayload is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(SubscribeMessage.SubscribePayload))

        then:
        noExceptionThrown()
    }

    void "CompleteMessage is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(CompleteMessage))

        then:
        noExceptionThrown()
    }

    void "CompleteMessage is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(CompleteMessage))

        then:
        noExceptionThrown()
    }

    void "ErrorMessage is annotated with @Serdeable.Deserializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getDeserializableIntrospection(Argument.of(ErrorMessage))

        then:
        noExceptionThrown()
    }

    void "ErrorMessage is annotated with @Serdeable.Serializable"() {
        given:
        SerdeIntrospections serdeIntrospections = beanContext.getBean(SerdeIntrospections)

        when:
        serdeIntrospections.getSerializableIntrospection(Argument.of(ErrorMessage))

        then:
        noExceptionThrown()
    }

    void "SubscribeMessage deserializes Apollo-style payload with extensions field"() {
        given:
        String json = '''
{
  "id": "1",
  "type": "subscribe",
  "payload": {
    "variables": {
      "marketId": "BTCUSD"
    },
    "extensions": {
      "persistedQuery": {
        "version": 1,
        "sha256Hash": "abc123"
      }
    },
    "operationName": "TickerForTradeSubscription",
    "query": "subscription TickerForTradeSubscription($marketId: String!) { ticker(symbol: $marketId) { symbol } }"
  }
}
'''

        when:
        Message message = graphQLJsonSerializer.deserialize(json, Message)

        then:
        message instanceof SubscribeMessage

        with((message as SubscribeMessage).subscribePayload) {
            query == "subscription TickerForTradeSubscription(\$marketId: String!) { ticker(symbol: \$marketId) { symbol } }"
            operationName == "TickerForTradeSubscription"
            variables == [marketId: "BTCUSD"]
            extensions.persistedQuery.sha256Hash == "abc123"
            (extensions.persistedQuery.version as Number).intValue() == 1
        }
    }
}
