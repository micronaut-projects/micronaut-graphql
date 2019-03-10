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

package io.micronaut.configuration.graphql;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Collections;
import java.util.Map;

import static io.micronaut.configuration.graphql.GraphQLConfiguration.PREFIX;

/**
 * Configuration properties for GraphQL.
 *
 * @author Marcel Overdijk
 * @author James Kleeh
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
    protected GraphQLSubscriptionsConfiguration graphQLSubscriptionsConfiguration = new GraphQLSubscriptionsConfiguration();
    protected GraphiQLConfiguration graphiQLConfiguration = new GraphiQLConfiguration();

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

    /**
     * Returns the GraphQL subscriptions configuration.
     *
     * @return the GraphQL subscriptions configuration
     */
    public GraphQLSubscriptionsConfiguration getGraphQLSubscriptionsConfiguration() {
        return graphQLSubscriptionsConfiguration;
    }

    /**
     * Returns the GraphiQL configuration.
     *
     * @return the GraphiQL configuration
     */
    public GraphiQLConfiguration getGraphiQLConfiguration() {
        return graphiQLConfiguration;
    }

    /**
     * Configuration properties for GraphQL subscriptions.
     */
    @ConfigurationProperties(GraphQLSubscriptionsConfiguration.PREFIX)
    public static class GraphQLSubscriptionsConfiguration implements Toggleable {

        /**
         * The prefix to use for all GraphQL subscriptions configuration properties.
         */
        public static final String PREFIX = "subscriptions";

        /**
         * The configuration name whether GraphQL subscriptions is enabled.
         */
        public static final String ENABLED = GraphQLConfiguration.PREFIX + "." + PREFIX + ".enabled";

        /**
         * The default enabled value.
         */
        public static final boolean DEFAULT_ENABLED = false;

        /**
         * The configuration name of the GraphQL subscriptions path.
         */
        public static final String PATH = PREFIX + ".path";

        /**
         * The default GraphQL subscriptions path.
         */
        public static final String DEFAULT_PATH = GraphQLConfiguration.DEFAULT_PATH;

        protected boolean enabled = DEFAULT_ENABLED;
        protected String path = DEFAULT_PATH;

        /**
         * Returns whether GraphQL subscriptions is enabled.
         *
         * @return whether GraphQL subscriptions is enabled
         */
        @Override
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Returns the GraphQL subscriptions path.
         *
         * @return the GraphQL subscriptions path
         */
        public String getPath() {
            return path;
        }
    }

    /**
     * Configuration properties for GraphiQL.
     */
    @ConfigurationProperties(GraphiQLConfiguration.PREFIX)
    public static class GraphiQLConfiguration implements Toggleable {

        /**
         * The prefix to use for all GraphiQL configuration properties.
         */
        public static final String PREFIX = "graphiql";

        /**
         * The configuration name whether GraphiQL is enabled.
         */
        public static final String ENABLED = GraphQLConfiguration.PREFIX + "." + PREFIX + ".enabled";

        /**
         * The default enabled value.
         */
        public static final boolean DEFAULT_ENABLED = false;

        /**
         * The configuration name of the GraphiQL path.
         */
        public static final String PATH = PREFIX + ".path";

        /**
         * The default GraphiQL path.
         */
        public static final String DEFAULT_PATH = "/graphiql";

        /**
         * The configuration name of the GraphiQL template path.
         */
        public static final String TEMPLATE_PATH = PREFIX + ".template-path";

        /**
         * The default GraphiQL template path.
         */
        public static final String DEFAULT_TEMPLATE_PATH = "classpath:graphiql/index.html";

        /**
         * The configuration name of the GraphiQL template parameters.
         */
        public static final String TEMPLATE_PARAMETERS = PREFIX + ".template-parameters";

        /**
         * The default GraphiQL template parameters.
         */
        public static final Map<String, String> DEFAULT_TEMPLATE_PARAMETERS = Collections.EMPTY_MAP;

        /**
         * The configuration name of the GraphiQL page title.
         */
        public static final String PAGE_TITLE = PREFIX + ".page-title";

        /**
         * The default GraphiQL page title.
         */
        public static final String DEFAULT_PAGE_TITLE = "GraphiQL";

        protected boolean enabled = DEFAULT_ENABLED;
        protected String path = DEFAULT_PATH;
        protected String templatePath = DEFAULT_TEMPLATE_PATH;
        protected Map<String, String> templateParameters = DEFAULT_TEMPLATE_PARAMETERS;
        protected String pageTitle = DEFAULT_PAGE_TITLE;

        /**
         * Returns whether GraphiQL is enabled.
         *
         * @return whether GraphiQL is enabled
         */
        @Override
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Returns the GraphiQL path.
         *
         * @return the GraphiQL path
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns the GraphiQL template path.
         *
         * @return the GraphiQL template path
         */
        public String getTemplatePath() {
            return templatePath;
        }

        /**
         * Returns the GraphiQL template parameters to be substituted in the template.
         *
         * @return the GraphiQL template parameters
         */
        public Map<String, String> getTemplateParameters() {
            return templateParameters;
        }

        /**
         * Returns the GraphiQL page title.
         *
         * @return the GraphiQL page title
         */
        public String getPageTitle() {
            return pageTitle;
        }
    }
}
