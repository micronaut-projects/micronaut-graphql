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

package io.micronaut.configuration.graphql

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource
import io.micronaut.websocket.annotation.ServerWebSocket
import spock.lang.Specification

/**
 * @author Marcel Overdijk
 * @since 1.0
 */
class GraphQLSubscriptionsConfigurationSpec extends Specification {

    void "test graphql subscriptions disabled by default"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.start()

        expect:
        !context.containsBean(GraphQLSubscriptionsWebSocket)

        cleanup:
        context.close()
    }

    void "test graphql subscriptions enabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.subscriptions.enabled": true]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLSubscriptionsWebSocket)
        context.getBeanDefinition(GraphQLSubscriptionsWebSocket).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/graphql"

        cleanup:
        context.close()
    }

    void "test custom graphql subscriptions path"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.subscriptions.enabled": true,
                 "graphql.subscriptions.path"   : "/custom-subscriptions"]
        ))
        context.start()

        expect:
        context.containsBean(GraphQLSubscriptionsWebSocket)
        context.getBeanDefinition(GraphQLSubscriptionsWebSocket).getAnnotation(ServerWebSocket).getRequiredValue(String) == "/custom-subscriptions"

        cleanup:
        context.close()
    }

    void "test graphql subscriptions disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.subscriptions.enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphQLSubscriptionsWebSocket)

        cleanup:
        context.close()
    }
}
