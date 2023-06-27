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
package io.micronaut.configuration.graphql.ws.apollo;

import io.micronaut.configuration.graphql.GraphQLConfiguration;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

/**
 * Configuration properties for using a web socket with GraphQL.
 *
 * @author Gerard Klijs
 * @since 1.3
 * @deprecated The Apollo subscriptions-transport-ws protocol is deprecated and its usage should be replaced with the new graphql-ws implementation.
 */
@Deprecated(since = "4.0")
@ConfigurationProperties(GraphQLConfiguration.PREFIX + "." + GraphQLApolloWsConfiguration.PREFIX)
public class GraphQLApolloWsConfiguration implements Toggleable {

    /**
     * The prefix to use for all GraphQL websocket configuration properties.
     */
    public static final String PREFIX = "graphql-apollo-ws";

    /**
     * The configuration name whether the GraphQL websocket is enabled.
     */
    public static final String ENABLED_CONFIG = GraphQLConfiguration.PREFIX + "." + PREFIX + ".enabled";

    /**
     * The default enabled value.
     */
    public static final boolean DEFAULT_ENABLED = false;

    /**
     * The configuration name of the GraphQL websocket path.
     */
    public static final String PATH_CONFIG = PREFIX + ".path";

    /**
     * The default GraphQL websocket path.
     */
    public static final String DEFAULT_PATH = "/graphql-ws";

    /**
     * The configuration name of the GraphQL keep alive enabled path..
     */
    public static final String KEEP_ALIVE_ENABLED_CONFIG = PREFIX + ".keep-alive-enabled";

    /**
     * The default keep alive enabled value.
     */
    public static final boolean DEFAULT_KEEP_ALIVE_ENABLED = true;

    /**
     * The configuration name of the GraphQL keep alive interval path..
     */
    public static final String KEEP_ALIVE_INTERVAL_CONFIG = PREFIX + ".keep-alive-interval";

    /**
     * The default keep alive interval value.
     */
    public static final String DEFAULT_KEEP_ALIVE_INTERVAL = "15s";

    protected boolean enabled = DEFAULT_ENABLED;
    protected String path = DEFAULT_PATH;
    protected boolean keepAliveEnabled = DEFAULT_KEEP_ALIVE_ENABLED;
    protected String keepAliveInterval = DEFAULT_KEEP_ALIVE_INTERVAL;

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
     * Sets whether GraphQL websocket is enabled.
     *
     * @param enabled whether GraphQL websocket is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
     * Sets the GraphQL websocket path.
     *
     * @param path the GraphQL websocket path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns whether GraphQL websocket keep alive is enabled.
     *
     * @return whether GraphQL websocket keep alive is enabled
     */
    public boolean isKeepAliveEnabled() {
        return keepAliveEnabled;
    }

    /**
     * Sets whether GraphQL websocket keep alive is enabled.
     *
     * @param keepAliveEnabled whether GraphQL websocket keep alive is enabled
     */
    public void setKeepAliveEnabled(boolean keepAliveEnabled) {
        this.keepAliveEnabled = keepAliveEnabled;
    }

    /**
     * Returns the GraphQL keep alive interval in seconds.
     *
     * @return the GraphQL keep alive interval in seconds
     */
    public String getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * Sets the GraphQL keep alive interval in seconds.
     *
     * @param keepAliveInterval the GraphQL keep alive interval in seconds
     */
    public void setKeepAliveInterval(String keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }
}
