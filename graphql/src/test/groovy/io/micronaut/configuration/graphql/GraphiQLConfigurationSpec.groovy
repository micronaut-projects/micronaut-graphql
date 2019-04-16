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
import io.micronaut.http.annotation.Controller
import spock.lang.Specification

/**
 * @author Marcel Overdijk
 * @since 1.0
 */
class GraphiQLConfigurationSpec extends Specification {

    void "test graphiql disabled by default"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.start()

        expect:
        !context.containsBean(GraphiQLController)

        cleanup:
        context.close()
    }

    void "test graphiql enabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphiql.enabled": true]
        ))
        context.start()

        expect:
        context.containsBean(GraphiQLController)
        context.getBeanDefinition(GraphiQLController).getAnnotation(Controller).getRequiredValue(String) == "/graphiql"

        cleanup:
        context.close()
    }

    void "test custom graphiql path"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphiql.enabled": true,
                 "graphql.graphiql.path"   : "/custom-graphiql"]
        ))
        context.start()

        expect:
        context.containsBean(GraphiQLController)
        context.getBeanDefinition(GraphiQLController).getAnnotation(Controller).getRequiredValue(String) == "/custom-graphiql"
        context.getBean(GraphQLConfiguration).graphiql.path == "/custom-graphiql"

        cleanup:
        context.close()
    }

    void "test graphiql disabled"() {
        given:
        ApplicationContext context = new DefaultApplicationContext(Environment.TEST)
        context.environment.addPropertySource(PropertySource.of(
                ["graphql.graphiql.enabled": false]
        ))
        context.start()

        expect:
        !context.containsBean(GraphiQLController)

        cleanup:
        context.close()
    }
}
