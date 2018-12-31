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

package io.micronaut.graphql;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import static io.micronaut.graphql.GraphiQLConfiguration.PREFIX;

/**
 * Configuration properties for GraphQL.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@ConfigurationProperties(PREFIX)
public class GraphQLConfiguration implements Toggleable {

    /**
     * The prefix to use for all GraphQL configuration properties.
     */
    public static final String PREFIX = "graphql";

    /**
     * The configuration name whether GraphQL is enabled.
     */
    public static final String ENABLED = PREFIX + ".enabled";

    /**
     * The default enabled value.
     */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * The configuration name of the GraphQL path.
     */
    public static final String PATH = PREFIX + ".path";

    /**
     * The default GraphQL path.
     */
    public static final String DEFAULT_PATH = "/graphql";

    protected boolean enabled = DEFAULT_ENABLED;
    protected String path = DEFAULT_PATH;

    /**
     * Returns whether GraphQL is enabled.
     *
     * @return whether GraphQL is enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the GraphQL path.
     *
     * @return the GraphQL path
     */
    public String getPath() {
        return path;
    }
}
