/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.graphql

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.Scalars
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.runtime.context.scope.Refreshable
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

class DefaultGraphQLInvocationSpec extends Specification {

    private static final GraphQLInvocationData INVOCATION_DATA = new GraphQLInvocationData("{ foo }", null, [:])

    void "bean context constructor resolves refreshed GraphQL bean"() {
        given:
        RefreshableGraphQLFactory.beanContextVersion.set(0)
        ApplicationContext context = ApplicationContext.run(["spec.name": "bean-context-refresh"])
        DefaultGraphQLInvocation invocation = new DefaultGraphQLInvocation(context, passthroughCustomizer(), null)

        expect:
        execute(invocation).data.foo == "bean-context-1"

        when:
        context.publishEvent(new RefreshEvent())

        then:
        execute(invocation).data.foo == "bean-context-2"

        cleanup:
        context.close()
    }

    void "bean context constructor resolves non-refreshable GraphQL bean"() {
        given:
        ApplicationContext context = ApplicationContext.run(["spec.name": "bean-context-direct"])
        DefaultGraphQLInvocation invocation = new DefaultGraphQLInvocation(context, passthroughCustomizer(), null)

        expect:
        execute(invocation).data.foo == "bean-context-direct"

        cleanup:
        context.close()
    }

    void "deprecated constructor uses supplied GraphQL instance"() {
        given:
        DefaultGraphQLInvocation invocation = new DefaultGraphQLInvocation(graphQL("direct"), passthroughCustomizer(), null)

        expect:
        execute(invocation).data.foo == "direct"
    }

    private ExecutionResult execute(DefaultGraphQLInvocation invocation) {
        Mono.from(invocation.invoke(INVOCATION_DATA, HttpRequest.GET("/graphql"), null)).block()
    }

    private GraphQLExecutionInputCustomizer passthroughCustomizer() {
        { ExecutionInput executionInput, HttpRequest httpRequest, Object httpResponse ->
            Publishers.just(executionInput)
        } as GraphQLExecutionInputCustomizer
    }

    private static GraphQL graphQL(String value) {
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(GraphQLObjectType.newObject()
                        .name("Query")
                        .field {
                            it.name("foo")
                            it.type(Scalars.GraphQLString)
                            it.dataFetcher { value }
                        }
                        .build())
                .build()
        GraphQL.newGraphQL(schema).build()
    }

    @Factory
    static class RefreshableGraphQLFactory {
        static final AtomicInteger beanContextVersion = new AtomicInteger()

        @Bean
        @Refreshable
        @Requires(property = "spec.name", value = "bean-context-refresh")
        GraphQL beanContextGraphQL() {
            graphQL("bean-context-${beanContextVersion.incrementAndGet()}")
        }

        @Bean
        @Requires(property = "spec.name", value = "bean-context-direct")
        GraphQL beanContextDirectGraphQL() {
            graphQL("bean-context-direct")
        }
    }
}
