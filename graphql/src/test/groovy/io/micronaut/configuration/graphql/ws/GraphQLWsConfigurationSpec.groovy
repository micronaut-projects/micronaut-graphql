package io.micronaut.configuration.graphql.ws


import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.websocket.annotation.ServerWebSocket
import spock.lang.Specification

import java.time.Duration

class GraphQLWsConfigurationSpec extends Specification {

    void "test graphql websocket disabled by default"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.start()

        expect:
        !context.containsBean(GraphQLWsHandler)

        cleanup:
        context.close()
    }

    void "test graphql websocket enabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-ws.enabled": true]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLWsHandler)
        context.getBeanDefinition(GraphQLWsHandler).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/graphql-ws"
        GraphQLWsConfiguration graphQLWsConfiguration = context.getBean(GraphQLWsConfiguration)
        graphQLWsConfiguration.path == "/graphql-ws"

        and:
        graphQLWsConfiguration.enabled
        graphQLWsConfiguration.connectionInitWaitTimeout == Duration.ofSeconds(15L)

        cleanup:
        context.close()
    }

    void "test graphql websocket enabled with custom connection timeout"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-ws.enabled"                     : true,
                 "graphql.graphql-ws.connection-init-wait-timeout": "30s"]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLWsHandler)
        context.getBeanDefinition(GraphQLWsHandler).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/graphql-ws"
        GraphQLWsConfiguration graphQLWsConfiguration = context.getBean(GraphQLWsConfiguration)
        graphQLWsConfiguration.path == "/graphql-ws"

        and:
        graphQLWsConfiguration.enabled
        graphQLWsConfiguration.connectionInitWaitTimeout == Duration.ofSeconds(30L)

        cleanup:
        context.close()
    }
    void "test custom path"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-ws.enabled": true,
                 "graphql.graphql-ws.path"   : "/custom-graphql-ws"]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLWsHandler)
        context.getBeanDefinition(GraphQLWsHandler).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/custom-graphql-ws"
        context.getBean(GraphQLWsConfiguration).path == "/custom-graphql-ws"

        cleanup:
        context.close()
    }

    void "test graphql websocket disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-ws.enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphQLWsHandler)

        cleanup:
        context.close()
    }
}
