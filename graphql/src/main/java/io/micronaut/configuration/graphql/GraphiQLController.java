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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DefaultPropertyPlaceholderResolver;
import io.micronaut.context.env.PropertyPlaceholderResolver;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.value.MapPropertyResolver;
import io.micronaut.core.value.PropertyResolver;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.MediaType.TEXT_HTML;

/**
 * The GraphiQL controller serving the GraphiQL HTML page.
 *
 * @author Marcel Overdijk
 * @since 1.0
 */
@Controller("${" + GraphQLConfiguration.PREFIX + "." + GraphQLConfiguration.GraphiQLConfiguration.PATH + ":"
        + GraphQLConfiguration.GraphiQLConfiguration.DEFAULT_PATH + "}")
@Requires(property = GraphQLConfiguration.PREFIX + "." + GraphQLConfiguration.GraphiQLConfiguration.ENABLED,
        value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class GraphiQLController {

    private final GraphQLConfiguration graphQLConfiguration;
    private final GraphQLConfiguration.GraphiQLConfiguration graphiQLConfiguration;
    private final ResourceResolver resourceResolver;

    private String cachedTemplate;

    /**
     * Default constructor.
     *
     * @param graphQLConfiguration the {@link GraphQLConfiguration} instance
     * @param resourceResolver     the {@link ResourceResolver} instance
     */
    public GraphiQLController(GraphQLConfiguration graphQLConfiguration, ResourceResolver resourceResolver) {
        this.graphQLConfiguration = graphQLConfiguration;
        this.graphiQLConfiguration = graphQLConfiguration.getGraphiQLConfiguration();
        this.resourceResolver = resourceResolver;
    }

    /**
     * Handles the incoming GraphiQL {@code GET} requests.
     *
     * @return the GraphiQL page
     */
    @Get(produces = TEXT_HTML + ";charset=UTF-8")
    public String get() {
        if (cachedTemplate == null) {
            synchronized (this) {
                if (cachedTemplate == null) {

                    String rawTemplate = loadTemplate(graphiQLConfiguration.getTemplatePath());
                    Map<String, String> parameters = new HashMap<>();
                    parameters.put("graphqlPath", graphQLConfiguration.getPath());
                    parameters.put("pageTitle", graphiQLConfiguration.getPageTitle());
                    if (graphiQLConfiguration.getTemplateParameters() != null) {
                        graphiQLConfiguration.getTemplateParameters().forEach((name, value) ->
                                // De-capitalize and de-hyphenate the parameter names.
                                // Otherwise `graphiql.template-parameters.magicWord` would be put as `magic-word` in the parameters map
                                // as Micronaut normalises properties and stores them lowercase hyphen separated.
                                parameters.put(NameUtils.decapitalize(NameUtils.dehyphenate(name)), value));
                    }
                    cachedTemplate = replaceParameters(rawTemplate, parameters);
                }
            }
        }
        return cachedTemplate;
    }

    private String loadTemplate(final String templateFile) {
        Optional<InputStream> template = resourceResolver.getResourceAsStream(templateFile);
        if (template.isPresent()) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(template.get(), StandardCharsets.UTF_8))) {
                return IOUtils.readText(in);
            } catch (IOException e) {
                throw new ConfigurationException("Cannot read GraphiQL template: " + templateFile, e);
            }
        } else {
            throw new ConfigurationException("Cannot find GraphiQL template: " + templateFile);
        }
    }

    private String replaceParameters(final String str, final Map<String, String> parameters) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(parameters);
        PropertyResolver propertyResolver = new MapPropertyResolver(map);
        PropertyPlaceholderResolver propertyPlaceholderResolver = new DefaultPropertyPlaceholderResolver(propertyResolver);
        return propertyPlaceholderResolver.resolvePlaceholders(str).get();
    }
}
