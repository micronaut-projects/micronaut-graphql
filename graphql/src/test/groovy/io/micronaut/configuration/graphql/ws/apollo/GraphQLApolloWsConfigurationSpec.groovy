package io.micronaut.configuration.graphql.ws.apollo

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.websocket.annotation.ServerWebSocket
import jakarta.inject.Singleton
import spock.lang.Specification
/**
 * @author Gerard Klijs
 * @since 1.3
 */
class GraphQLApolloWsConfigurationSpec extends Specification {

    void "test graphql websocket disabled by default"() {
        given:
        ApplicationContext context = ApplicationContext.run(["spec.name": GraphQLApolloWsConfigurationSpec.simpleName], Environment.TEST)

        expect:
        !context.containsBean(GraphQLApolloWsController)

        cleanup:
        context.close()
    }

    void "test graphql websocket enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run(["spec.name": GraphQLApolloWsConfigurationSpec.simpleName,
                                                             "graphql.graphql-apollo-ws.enabled": true], Environment.TEST)

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
        ApplicationContext context = ApplicationContext.run(["spec.name": GraphQLApolloWsConfigurationSpec.simpleName,
                                                             "graphql.graphql-apollo-ws.enabled": true,
                                                             "graphql.graphql-apollo-ws.path"   : "/custom-graphql-ws"], Environment.TEST)

        expect:
        context.containsBean(GraphQLApolloWsController)
        context.getBeanDefinition(GraphQLApolloWsController).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/custom-graphql-ws"
        context.getBean(GraphQLApolloWsConfiguration).path == "/custom-graphql-ws"

        cleanup:
        context.close()
    }

    void "test graphql websocket disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run(["spec.name": GraphQLApolloWsConfigurationSpec.simpleName,
                                                             "graphql.graphql-apollo-ws.enabled": false], Environment.TEST)

        expect:
        !context.containsBean(GraphQLApolloWsController)

        cleanup:
        context.close()
    }

    void "test graphql websocket keepalive disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run(["spec.name": GraphQLApolloWsConfigurationSpec.simpleName,
                                                             "graphql.graphql-apollo-ws.keep-alive-enabled": false], Environment.TEST)

        expect:
        !context.getBean(GraphQLApolloWsConfiguration).enabled

        cleanup:
        context.close()
    }

    void "test bean not created when graphql websocket keepalive disabled"() {
        given:
        ApplicationContext context = ApplicationContext.run(["spec.name": GraphQLApolloWsConfigurationSpec.simpleName,
                                                             "graphql.graphql-apollo-ws.keep-alive-enabled": false], Environment.TEST)

        expect:
        !context.containsBean(GraphQLApolloWsKeepAlive)

        cleanup:
        context.close()
    }

    void "test graphql websocket keepalive different interval"() {
        given:
        ApplicationContext context = ApplicationContext.run(["spec.name": GraphQLApolloWsConfigurationSpec.simpleName,
                                                             "graphql.graphql-apollo-ws.keep-alive-interval": "1s"], Environment.TEST)

        expect:
        context.getBean(GraphQLApolloWsConfiguration).keepAliveInterval == "1s"

        cleanup:
        context.close()
    }

    @Factory
    static class GraphQLFactory {

        @Bean
        @Singleton
        @Requires(property = "spec.name", value = "GraphQLApolloWsConfigurationSpec")
        GraphQL graphQL() {
            def schema = GraphQLSchema.newSchema().build()
            GraphQL.newGraphQL(schema).build()
        }
    }
}
