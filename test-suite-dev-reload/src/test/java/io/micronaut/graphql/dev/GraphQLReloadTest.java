/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.graphql.dev;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.DevelopmentMode;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import io.micronaut.inject.BeanDefinitionReference;
import io.micronaut.runtime.server.EmbeddedServer;
import io.netty.util.internal.PlatformDependent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs a GraphQL application through the development runtime. A changed schema file under {@code src/main/resources}
 * and a change of the configuration under {@code graphql} are applied in place, by recreating the {@code GraphQL} bean,
 * and a change of the GraphiQL page by recreating the GraphiQL controller; a changed data fetcher starts a new
 * generation, which answers with the new data. GraphQL over WebSocket follows both: an open connection runs its next
 * operation on the recreated bean, and a restart closes it for the client to connect again. Nothing of GraphQL keeps a
 * retired generation reachable.
 */
class GraphQLReloadTest {

    private static final String RELOADER = "io.micronaut.graphql.dev.DevelopmentGraphQLReloader";

    private static final String PROPERTIES = """
        micronaut.server.port=-1
        graphql.graphiql.enabled=true
        graphql.graphiql.page-title=%s
        graphql.graphql-ws.enabled=true
        graphql.greeting.text=%s
        """;

    private static final String FETCHER = """
        package example;

        @jakarta.inject.Singleton
        public class HelloFetcher implements graphql.schema.DataFetcher<String> {
            @Override
            public String get(graphql.schema.DataFetchingEnvironment environment) {
                return "%s";
            }
        }
        """;

    private static final String FACTORY = """
        package example;

        import graphql.GraphQL;
        import graphql.schema.idl.RuntimeWiring;
        import graphql.schema.idl.SchemaGenerator;
        import graphql.schema.idl.SchemaParser;
        import io.micronaut.context.annotation.Factory;
        import io.micronaut.core.io.ResourceResolver;
        import jakarta.inject.Singleton;

        import java.io.InputStreamReader;
        import java.io.Reader;
        import java.nio.charset.StandardCharsets;

        @Factory
        public class GraphQLFactory {
            @Singleton
            public GraphQL graphQL(ResourceResolver resourceResolver, HelloFetcher hello, GreetingConfiguration configuration) throws java.io.IOException {
                // read once, as the GraphQL bean is built: the configuration bean is bound again in place, the GraphQL bean is not
                String greeting = configuration.getText();
                try (Reader schema = new InputStreamReader(resourceResolver.getResourceAsStream("classpath:graphql/schema.graphqls").orElseThrow(), StandardCharsets.UTF_8)) {
                    RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                        .type("Query", type -> type
                            .defaultDataFetcher(environment -> environment.getField().getName())
                            .dataFetcher("hello", hello)
                            .dataFetcher("greeting", environment -> greeting))
                        .build();
                    return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(schema), wiring)).build();
                }
            }
        }
        """;

    private static final String GREETING_CONFIGURATION = """
        package example;

        @io.micronaut.context.annotation.ConfigurationProperties("graphql.greeting")
        public class GreetingConfiguration {
            private String text = "hi";

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }
        """;

    private final HttpClient client = HttpClient.newHttpClient();

    @TempDir
    Path project;

    @BeforeAll
    static void initializeNetty() {
        // Netty records why it cannot use Unsafe in a static exception, whose stack trace holds the classes on the
        // stack when Netty is first loaded. Loaded first by the application's main method, that is generation one's
        // Application class, which Netty would keep reachable for the life of the process: loaded here, it is this test
        PlatformDependent.hasUnsafe();
    }

    @Test
    void graphQLFollowsAReloadAndLeavesNoRetiredGenerationReachable() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            harness.property("micronaut.server.port", "-1")
                .property("graphql.graphiql.enabled", "true")
                .property("graphql.graphiql.page-title", "Before")
                .property("graphql.graphql-ws.enabled", "true")
                .property("graphql.greeting.text", "hi");
            // the schema is under the configuration root, src/main/resources, whose other files the development runtime
            // reports to resource watches too
            harness.resource("graphql/schema.graphqls", "type Query { hello: String greeting: String }");
            harness.source("example.HelloFetcher", FETCHER.formatted("one"));
            harness.source("example.GreetingConfiguration", GREETING_CONFIGURATION);
            harness.source("example.GraphQLFactory", FACTORY);
            harness.start();
            assertReloaderPresent(harness.context());

            assertEquals("{\"data\":{\"hello\":\"one\",\"greeting\":\"hi\"}}", query(harness, "{ hello greeting }"));
            assertTrue(graphiql(harness).contains("<title>Before</title>"));
            GraphQLWs ws = GraphQLWs.connect(client, uri(harness, "/graphql-ws"));
            assertTrue(ws.execute("1", "{ hello }").contains("\"hello\":\"one\""));

            // the schema file gains a field: the GraphQL bean is built again from it, in the same generation
            int generation = harness.generation();
            harness.resource("graphql/schema.graphqls", "type Query { hello: String greeting: String added: String }");
            harness.reload();
            assertEquals(generation, harness.generation());
            assertEquals("{\"data\":{\"hello\":\"one\",\"added\":\"added\"}}", query(harness, "{ hello added }"));
            // an open GraphQL over WebSocket connection runs its next operation on the new schema
            assertTrue(ws.execute("2", "{ added }").contains("\"added\":\"added\""));

            // configuration under graphql that the application builds the GraphQL bean from, through a configuration bean
            // bound again in place: the GraphQL bean is built again from it, in the same generation
            harness.resource("application.properties", PROPERTIES.formatted("Before", "hello"));
            harness.reload();
            assertEquals(generation, harness.generation());
            assertEquals("{\"data\":{\"greeting\":\"hello\"}}", query(harness, "{ greeting }"));
            assertTrue(ws.execute("3", "{ greeting }").contains("\"greeting\":\"hello\""));

            // the GraphiQL controller renders its page once: it is created again, in the same generation, and the
            // development router serves the next request from the new one
            harness.resource("application.properties", PROPERTIES.formatted("After", "hello"));
            harness.reload();
            assertEquals(generation, harness.generation());
            assertTrue(graphiql(harness).contains("<title>After</title>"));
            assertEquals("{\"data\":{\"added\":\"added\",\"greeting\":\"hello\"}}", query(harness, "{ added greeting }"));
            // the GraphQL over WebSocket connection stays open
            assertTrue(ws.execute("4", "{ hello }").contains("\"hello\":\"one\""));

            // the GraphQL and GraphiQL endpoints move: their controllers are created again, in the same generation, and
            // the development router routes them at the new paths
            harness.resource("application.properties", PROPERTIES.formatted("After", "hello") + "graphql.path=/gql\ngraphql.graphiql.path=/giql\n");
            harness.reload();
            assertEquals(generation, harness.generation());
            assertEquals("{\"data\":{\"greeting\":\"hello\"}}", query(harness, "/gql", "{ greeting }"));
            String page = get(harness, "/giql", 200);
            assertTrue(page.contains("'/giql'") && page.contains("'/gql'"), "The GraphiQL page names the new paths");
            get(harness, "/graphiql", 404);
            assertEquals(404, post(harness, "/graphql", "{ greeting }").statusCode());
            assertTrue(ws.execute("5", "{ greeting }").contains("\"greeting\":\"hello\""));

            // and move back
            harness.resource("application.properties", PROPERTIES.formatted("After", "hello"));
            harness.reload();
            assertEquals(generation, harness.generation());
            assertTrue(graphiql(harness).contains("'/graphql'"));
            assertEquals(404, post(harness, "/gql", "{ greeting }").statusCode());

            // the data fetcher changes: a new generation answers with the new data
            harness.source("example.HelloFetcher", FETCHER.formatted("two"));
            harness.reload();
            assertTrue(harness.generation() > generation);
            assertReloaderPresent(harness.context());
            assertEquals("{\"data\":{\"hello\":\"two\",\"added\":\"added\"}}", query(harness, "{ hello added }"));
            assertTrue(ws.awaitClosed(), "The WebSocket connection to the retired generation is closed");
            GraphQLWs reconnected = GraphQLWs.connect(client, uri(harness, "/graphql-ws"));
            assertTrue(reconnected.execute("1", "{ hello }").contains("\"hello\":\"two\""));
            reconnected.close();

            // neither the GraphQL bean, the endpoints nor the development-only reloader keep a retired generation reachable
            releaseCoreResidual(harness.context());
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    @Test
    void theReloaderExistsInDevelopmentModeOnly() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertFalse(context.containsBean(Class.forName(RELOADER)));
        }
        // in development mode it exists while GraphQL is disabled too, to ask for the restart that enables it
        try (ApplicationContext context = ApplicationContext.run(Map.of(DevelopmentMode.PROPERTY, true, "graphql.enabled", false))) {
            assertTrue(context.containsBean(Class.forName(RELOADER)));
        }
    }

    /**
     * A residual of core, not of GraphQL: a bean definition of the parent tier keeps, on its static executable methods
     * ({@code $EXEC}), the environment of the last context that loaded it. The development runtime loads the definition
     * of the heartbeat task of discovery-core, which the WebSocket support brings, in one generation and not in the
     * next, so the environment of a retired generation stays on {@code $HeartbeatTask$Definition.$EXEC}. Loading it in
     * the current context configures it with the current environment.
     */
    @SuppressWarnings("unchecked")
    private static void releaseCoreResidual(ApplicationContext context) throws ReflectiveOperationException {
        // disabled, the definition is not among the context's references: it is created as the context would create it
        Class<?> type = Class.forName("io.micronaut.health.$HeartbeatTask$Definition", true, ApplicationContext.class.getClassLoader());
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        ((BeanDefinitionReference<Object>) constructor.newInstance()).load(context);
    }

    private String query(ReloadHarness harness, String query) throws IOException, InterruptedException {
        return query(harness, "/graphql", query);
    }

    private String query(ReloadHarness harness, String path, String query) throws IOException, InterruptedException {
        HttpResponse<String> response = post(harness, path, query);
        assertEquals(200, response.statusCode(), response.body());
        return response.body();
    }

    private HttpResponse<String> post(ReloadHarness harness, String path, String query) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri(harness, path))
            .header("Content-Type", "application/graphql")
            .POST(HttpRequest.BodyPublishers.ofString(query))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String graphiql(ReloadHarness harness) throws IOException, InterruptedException {
        return get(harness, "/graphiql", 200);
    }

    private String get(ReloadHarness harness, String path, int status) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri(harness, path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, response.statusCode(), response.body());
        return response.body();
    }

    private static URI uri(ReloadHarness harness, String path) {
        return harness.context().getBean(EmbeddedServer.class).getURI().resolve(path);
    }

    private static void assertReloaderPresent(ApplicationContext context) {
        // the bean that recreates the GraphQL bean exists in development mode only
        try {
            assertTrue(context.containsBean(Class.forName(RELOADER, true, context.getClassLoader())));
        } catch (ClassNotFoundException e) {
            throw new AssertionError(RELOADER + " is not on the classpath", e);
        }
    }

    /**
     * A client of the {@code graphql-transport-ws} protocol, enough to run one operation at a time.
     */
    private static final class GraphQLWs implements WebSocket.Listener {

        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final CompletableFuture<Integer> closed = new CompletableFuture<>();
        private final StringBuilder partial = new StringBuilder();
        private WebSocket socket;

        static GraphQLWs connect(HttpClient client, URI uri) throws Exception {
            GraphQLWs ws = new GraphQLWs();
            URI webSocketUri = new URI("ws", null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
            ws.socket = client.newWebSocketBuilder()
                .subprotocols("graphql-transport-ws")
                .buildAsync(webSocketUri, ws)
                .get(10, TimeUnit.SECONDS);
            ws.send("{\"type\":\"connection_init\"}");
            String ack = ws.next();
            assertTrue(ack.contains("connection_ack"), ack);
            return ws;
        }

        /**
         * Runs a query and returns its {@code next} message, once the operation completed.
         */
        String execute(String id, String query) throws Exception {
            send("{\"id\":\"" + id + "\",\"type\":\"subscribe\",\"payload\":{\"query\":\"" + query + "\"}}");
            String next = next();
            assertTrue(next.contains("\"next\""), next);
            String complete = next();
            assertTrue(complete.contains("\"complete\""), complete);
            return next;
        }

        boolean awaitClosed() throws Exception {
            try {
                closed.get(10, TimeUnit.SECONDS);
                return true;
            } catch (java.util.concurrent.TimeoutException e) {
                return false;
            }
        }

        void close() throws Exception {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").get(10, TimeUnit.SECONDS);
            awaitClosed();
        }

        private void send(String text) throws Exception {
            socket.sendText(text, true).get(10, TimeUnit.SECONDS);
        }

        private String next() throws InterruptedException {
            String message = messages.poll(10, TimeUnit.SECONDS);
            assertNotNull(message, "No message within 10 seconds");
            return message;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(statusCode);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closed.complete(-1);
        }
    }
}
