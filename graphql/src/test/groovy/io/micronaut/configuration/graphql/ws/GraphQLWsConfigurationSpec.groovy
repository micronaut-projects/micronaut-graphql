/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.graphql.ws

import io.micronaut.configuration.graphql.ws.GraphQLWsConfiguration
import io.micronaut.configuration.graphql.ws.GraphQLWsController
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.websocket.annotation.ServerWebSocket
import spock.lang.Specification

/**
 * @author Gerard Klijs
 * @since 1.3
 */
class GraphQLWsConfigurationSpec extends Specification {

    void "test graphql websocket disabled by default"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.start()

        expect:
        !context.containsBean(GraphQLWsController)

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
        context.containsBean(GraphQLWsController)
        context.getBeanDefinition(GraphQLWsController).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/graphql-ws"
        GraphQLWsConfiguration graphQLWsConfiguration = context.getBean(GraphQLWsConfiguration)
        graphQLWsConfiguration.path == "/graphql-ws"

        and:
        graphQLWsConfiguration.enabled
        graphQLWsConfiguration.keepAliveEnabled
        graphQLWsConfiguration.keepAliveInterval == "15s"

        cleanup:
        context.close()
    }

    void "test custom graphiql path"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-ws.enabled": true,
                 "graphql.graphql-ws.path"   : "/custom-graphql-ws"]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLWsController)
        context.getBeanDefinition(GraphQLWsController).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/custom-graphql-ws"
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
        !context.containsBean(GraphQLWsController)

        cleanup:
        context.close()
    }

    void "test graphql websocket keepalive disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-ws.keep-alive-enabled": false]
        ))
        context.start()

        expect:
        !context.getBean(GraphQLWsConfiguration).enabled

        cleanup:
        context.close()
    }

    void "test graphql websocket keepalive different interval"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphql-ws.keep-alive-interval": "1s"]
        ))
        context.start()

        expect:
        context.getBean(GraphQLWsConfiguration).keepAliveInterval == "1s"

        cleanup:
        context.close()
    }
}
