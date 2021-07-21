package io.micronaut.configuration.graphql.ws

import graphql.GraphQL
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeRuntimeWiring
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

import java.time.Duration

@Factory
class GraphQLWsFactory {

    @Bean
    @Requires(env = ["websocket", "keepalive"])
    GraphQL graphQL() {
        SchemaParser schemaParser = new SchemaParser()
        SchemaGenerator schemaGenerator = new SchemaGenerator()

        TypeDefinitionRegistry typeRegistry = schemaParser.parse(new InputStreamReader(
                getClass().getResourceAsStream("/websocket.graphql")))

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type(TypeRuntimeWiring.newTypeWiring("QueryRoot").dataFetcher("foo", new Foo()))
                .type(TypeRuntimeWiring.newTypeWiring("QueryRoot").dataFetcher("error", new Error()))
                .type(TypeRuntimeWiring.newTypeWiring("MutationRoot").dataFetcher("change", new Change()))
                .type(TypeRuntimeWiring.newTypeWiring("SubscriptionRoot").dataFetcher("counter", new Counter()))
                .build()

        return GraphQL
                .newGraphQL(schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring))
                .build()
    }

    class Foo implements DataFetcher<String> {
        @Override
        String get(DataFetchingEnvironment environment) throws Exception {
            return "bar"
        }
    }

    class Error implements DataFetcher<String> {
        @Override
        String get(DataFetchingEnvironment environment) throws Exception {
            throw new InstantiationException("No error present")
        }
    }

    class Change implements DataFetcher<Values> {
        private Values values = new Values()

        @Override
        Values get(DataFetchingEnvironment environment) throws Exception {
            String newValue = environment.getArgument("newValue")
            return values.change(newValue)
        }

        class Values {
            private List<String> old = []
            private String current = "Value_A"

            List<String> getOld() {
                return old
            }

            String getCurrent() {
                return current
            }

            Values change(String current) {
                old.add(this.current)
                this.current = current
                return this
            }
        }
    }

    class Counter implements DataFetcher<Publisher<Integer>> {
        @Override
        Publisher<Integer> get(DataFetchingEnvironment environment) throws Exception {
            return Flux.range(0, 3)
                    .delayElements(Duration.ofSeconds(1))
        }
    }
}
