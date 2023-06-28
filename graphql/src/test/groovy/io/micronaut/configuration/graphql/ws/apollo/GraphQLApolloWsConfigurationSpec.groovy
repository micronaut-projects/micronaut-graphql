package io.micronaut.configuration.graphql.ws.apollo

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.websocket.annotation.ServerWebSocket
import spock.lang.Specification

/**
 * @author Gerard Klijs
 * @since 1.3
 */
class GraphQLApolloWsConfigurationSpec extends Specification {

    void "test graphql websocket disabled by default"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.start()

        expect:
        !context.containsBean(GraphQLApolloWsController)

        cleanup:
        context.close()
    }

    void "test graphql websocket enabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-apollo-ws.enabled": true]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLApolloWsController)
        context.getBeanDefinition(GraphQLApolloWsController).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/graphql-ws"
        GraphQLApolloWsConfiguration graphQLApolloWsConfiguration = context.getBean(GraphQLApolloWsConfiguration)
        graphQLApolloWsConfiguration.path == "/graphql-ws"

        and:
        graphQLApolloWsConfiguration.enabled
        graphQLApolloWsConfiguration.keepAliveEnabled
        graphQLApolloWsConfiguration.keepAliveInterval == "15s"

        cleanup:
        context.close()
    }

    void "test custom path"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-apollo-ws.enabled": true,
                 "graphql.graphql-apollo-ws.path"   : "/custom-graphql-ws"]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLApolloWsController)
        context.getBeanDefinition(GraphQLApolloWsController).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/custom-graphql-ws"
        context.getBean(GraphQLApolloWsConfiguration).path == "/custom-graphql-ws"

        cleanup:
        context.close()
    }

    void "test graphql websocket disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-apollo-ws.enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphQLApolloWsController)

        cleanup:
        context.close()
    }

    void "test graphql websocket keepalive disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-apollo-ws.keep-alive-enabled": false]
        ))
        context.start()

        expect:
        !context.getBean(GraphQLApolloWsConfiguration).enabled

        cleanup:
        context.close()
    }

    void "test bean not created when graphql websocket keepalive disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-apollo-ws.keep-alive-enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphQLApolloWsKeepAlive)

        cleanup:
        context.close()
    }

    void "test graphql websocket keepalive different interval"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-apollo-ws.keep-alive-interval": "1s"]
        ))
        context.start()

        expect:
        context.getBean(GraphQLApolloWsConfiguration).keepAliveInterval == "1s"

        cleanup:
        context.close()
    }
}
