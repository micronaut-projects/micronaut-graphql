/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.configuration.graphql.ws;

import io.micronaut.configuration.graphql.GraphQLConfiguration;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.time.Duration;

/**
 * Configuration of the graphql-ws protocol support.
 *
 * @author Jeremy Grelle
 * @since 4.0
 */
@ConfigurationProperties(GraphQLConfiguration.PREFIX + "." + GraphQLWsConfiguration.PREFIX)
public class GraphQLWsConfiguration implements Toggleable {

    /**
     * The prefix to use for all GraphQL websocket configuration properties.
     */
    public static final String PREFIX = "graphql-ws";

    /**
     * The configuration name whether the GraphQL websocket is enabled.
     */
    public static final String ENABLED = GraphQLConfiguration.PREFIX + "." + PREFIX + ".enabled";

    /**
     * The default enabled value.
     */
    public static final boolean DEFAULT_ENABLED = false;

    /**
     * The configuration name of the GraphQL websocket path.
     */
    public static final String PATH = PREFIX + ".path";

    /**
     * The configuration name of the GraphQL websocket connection initialisation wait timeout.
     */
    public static final String CONNECTION_TIMEOUT = PREFIX + ".connection-init-wait-timeout";

    /**
     * The default GraphQL websocket path.
     */
    public static final String DEFAULT_PATH = "/graphql-ws";

    /**
     * The default connection initialisation wait timeout.
     */
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(15L);

    protected boolean enabled = DEFAULT_ENABLED;

    protected String path = DEFAULT_PATH;

    protected Duration connectionInitWaitTimeout = DEFAULT_CONNECTION_TIMEOUT;


    /**
     * Returns whether GraphQL websocket is enabled.
     *
     * @return whether GraphQL websocket is enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the GraphQL websocket path.
     *
     * @return the GraphQL websocket path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the connection intialisation wait timeout.
     *
     * @return the connection intialisation wait timeout
     */
    public Duration getConnectionInitWaitTimeout() {
        return connectionInitWaitTimeout;
    }
}
