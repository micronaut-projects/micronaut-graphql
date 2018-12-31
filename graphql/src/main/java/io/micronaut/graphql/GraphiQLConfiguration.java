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

import java.util.Collections;
import java.util.Map;

import static io.micronaut.graphql.GraphiQLConfiguration.PREFIX;

/**
 * Configuration properties for GraphiQL.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@ConfigurationProperties(PREFIX)
public class GraphiQLConfiguration implements Toggleable {

    /**
     * The prefix to use for all GraphiQL configuration properties.
     */
    public static final String PREFIX = "graphiql";

    /**
     * The configuration name whether GraphiQL is enabled.
     */
    public static final String ENABLED = PREFIX + ".enabled";

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
