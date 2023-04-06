/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.configuration.graphql;

import io.micronaut.configuration.graphql.apollo.ws.GraphQLWsConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DefaultPropertyPlaceholderResolver;
import io.micronaut.context.env.PropertyPlaceholderResolver;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.SupplierUtil;
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
import java.util.function.Supplier;

import static io.micronaut.http.MediaType.TEXT_HTML;

/**
 * The GraphiQL controller serving the GraphiQL HTML page.
 *
 * @author Marcel Overdijk
 * @author James Kleeh
 * @since 1.0
 */
@Controller("${" + GraphQLConfiguration.PREFIX + "." + GraphQLConfiguration.GraphiQLConfiguration.PATH + ":"
        + GraphQLConfiguration.GraphiQLConfiguration.DEFAULT_PATH + "}")
@Requires(property = GraphQLConfiguration.GraphiQLConfiguration.ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
public class GraphiQLController {

    private final GraphQLConfiguration graphQLConfiguration;
    private final GraphQLConfiguration.GraphiQLConfiguration graphiQLConfiguration;
    private final GraphQLWsConfiguration graphQLWsConfiguration;
    private final ResourceResolver resourceResolver;
    private final ConversionService conversionService;

    private final String rawTemplate;
    private final Supplier<String> resolvedTemplate;

    /**
     * Default constructor.
     *
     * @param graphQLConfiguration   the {@link GraphQLConfiguration} instance
     * @param graphQLWsConfiguration the {@link GraphQLWsConfiguration} instance
     * @param resourceResolver       the {@link ResourceResolver} instance
     * @param conversionService      the {@link ConversionService} instance
     */
    public GraphiQLController(
            GraphQLConfiguration graphQLConfiguration,
            GraphQLWsConfiguration graphQLWsConfiguration,
            ResourceResolver resourceResolver,
            ConversionService conversionService) {
        this.graphQLConfiguration = graphQLConfiguration;
        this.graphiQLConfiguration = graphQLConfiguration.getGraphiql();
        this.graphQLWsConfiguration = graphQLWsConfiguration;
        this.resourceResolver = resourceResolver;
        this.conversionService = conversionService;
        // Load the raw template (variables are not yet resolved).
        // This means we fail fast if the template cannot be loaded resulting in a ConfigurationException at startup.
        this.rawTemplate = loadTemplate(graphiQLConfiguration.getTemplatePath());
        this.resolvedTemplate = SupplierUtil.memoized(this::resolvedTemplate);
    }

    /**
     * Handles the incoming GraphiQL {@code GET} requests.
     *
     * @return the GraphiQL page
     */
    @Get(produces = TEXT_HTML + ";charset=UTF-8")
    public String get() {
        return resolvedTemplate.get();
    }

    private String loadTemplate(final String templateFile) {
        Optional<InputStream> template = resourceResolver.getResourceAsStream(templateFile);
        if (template.isPresent()) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(template.get(), StandardCharsets.UTF_8))) {
                return IOUtils.readText(in);
            } catch (IOException e) {
                throw new ConfigurationException("Cannot read GraphiQL template: " + templateFile, e);
            }
        } else {
            throw new ConfigurationException("Cannot find GraphiQL template: " + templateFile);
        }
    }

    private String resolvedTemplate() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("graphiqlVersion", graphiQLConfiguration.getVersion());
        parameters.put("graphqlPath", graphQLConfiguration.getPath());
        String graphQLWsPath = graphQLWsConfiguration.isEnabled() ? graphQLWsConfiguration.getPath() : "";
        parameters.put("graphqlWsPath", graphQLWsPath);
        parameters.put("pageTitle", graphiQLConfiguration.getPageTitle());
        parameters.put("graphiqlPath", graphiQLConfiguration.getPath());
        if (graphiQLConfiguration.getTemplateParameters() != null) {
            graphiQLConfiguration.getTemplateParameters().forEach((name, value) ->
                    // De-capitalize and de-hyphenate the parameter names.
                    // Otherwise `graphiql.template-parameters.magicWord` would be put as `magic-word` in the
                    // parameters map as Micronaut normalises properties and stores them lowercase hyphen separated.
                    parameters.put(NameUtils.decapitalize(NameUtils.dehyphenate(name)), value));
        }
        return replaceParameters(this.rawTemplate, parameters);
    }

    private String replaceParameters(final String str, final Map<String, String> parameters) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(parameters);
        PropertyResolver propertyResolver = new MapPropertyResolver(map);
        PropertyPlaceholderResolver propertyPlaceholderResolver =
                new DefaultPropertyPlaceholderResolver(propertyResolver, conversionService);
        return propertyPlaceholderResolver.resolvePlaceholders(str).get();
    }
}
