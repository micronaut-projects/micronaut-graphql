package io.micronaut.configuration.graphql.ws

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.core.util.StringUtils
import io.micronaut.websocket.annotation.ServerWebSocket
import jakarta.inject.Singleton
import spock.lang.Specification

import java.time.Duration

class GraphQLWsConfigurationSpec extends Specification {

    void "test graphql websocket disabled by default"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["spec.name": GraphQLWsConfigurationSpec.simpleName]
        ))
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
                ["spec.name": GraphQLWsConfigurationSpec.simpleName,
                 "graphql.graphql-ws.enabled": true]
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
                ["spec.name": GraphQLWsConfigurationSpec.simpleName,
                 "graphql.graphql-ws.enabled"                     : true,
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
                ["spec.name": GraphQLWsConfigurationSpec.simpleName,
                 "graphql.graphql-ws.enabled": true,
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
                ["spec.name": GraphQLWsConfigurationSpec.simpleName,
                 "graphql.graphql-ws.enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphQLWsHandler)

        cleanup:
        context.close()
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "graphql.factory", notEquals = StringUtils.FALSE)
        @Requires(property = "spec.name", value = "GraphQLWsConfigurationSpec")
        GraphQL graphQL() {
            def schema = GraphQLSchema.newSchema().build()
            GraphQL.newGraphQL(schema).build()
        }
    }
}
