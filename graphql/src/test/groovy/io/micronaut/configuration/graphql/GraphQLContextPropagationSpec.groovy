package io.micronaut.configuration.graphql

import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.io.ResourceResolver
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.context.ServerRequestContext
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Singleton
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.net.http.HttpRequest

class GraphQLContextPropagationSpec extends Specification {

    @AutoCleanup
    EmbeddedServer embeddedServer

    GraphQLContextPropagationClient graphQLClient

    def setup() {
        embeddedServer = ApplicationContext.run(
                EmbeddedServer,
                ["spec.name": GraphQLContextPropagationSpec.simpleName])
        graphQLClient = embeddedServer.applicationContext.getBean(GraphQLContextPropagationClient)
    }

    void "server request context is propagated to data fetcher"() {
        when:
        GraphQLResponseBody response = graphQLClient.hello("query { hello }")

        then:
        response
        response.getSpecification()["data"]["hello"] == "Hello World!"
    }

}
