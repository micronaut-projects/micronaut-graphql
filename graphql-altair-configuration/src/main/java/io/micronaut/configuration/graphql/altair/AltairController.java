package io.micronaut.configuration.graphql.altair;

import io.micronaut.configuration.graphql.GraphQLConfiguration;
import io.micronaut.configuration.graphql.ws.GraphQLWsConfiguration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DefaultPropertyPlaceholderResolver;
import io.micronaut.context.env.PropertyPlaceholderResolver;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.async.SupplierUtil;
import io.micronaut.core.io.IOUtils;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.naming.NameUtils;
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

@Controller("${" + AltairConfiguration.PREFIX + ":"
    + AltairConfiguration.DEFAULT_PATH + "}")
@Requires(property = AltairConfiguration.ENABLED)
public class AltairController {
    private final AltairConfiguration altairConfiguration;
    private final GraphQLConfiguration graphQLConfiguration;
    private final GraphQLWsConfiguration graphQLWsConfiguration;
    private final ResourceResolver resourceResolver;

    private final String rawTemplate;
    private final Supplier<String> resolvedTemplate;

    public AltairController(ResourceResolver resourceResolver, GraphQLWsConfiguration graphQLWsConfiguration, AltairConfiguration altairConfiguration, GraphQLConfiguration graphQLConfiguration) {
        this.altairConfiguration = altairConfiguration;
        this.graphQLConfiguration = graphQLConfiguration;
        this.resourceResolver = resourceResolver;
        this.graphQLWsConfiguration = graphQLWsConfiguration;

        this.rawTemplate = loadTemplate(altairConfiguration.getTemplatePath());
        this.resolvedTemplate = SupplierUtil.memoized(this::resolvedTemplate);
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

    /**
     * Handles the incoming GraphiQL {@code GET} requests.
     *
     * @return the GraphiQL page
     */
    @Get(produces = TEXT_HTML + ";charset=UTF-8")
    public String get() {
        return resolvedTemplate.get();
    }

    private String resolvedTemplate() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("altairVersion", altairConfiguration.getVersion());
        parameters.put("altairEndpoint", graphQLConfiguration.getPath());
        String graphQLWsPath = altairConfiguration.isEnabled() ? altairConfiguration.getPath() : "";
        parameters.put("altairWsPath", graphQLWsPath);
        parameters.put("altairPageTitle", altairConfiguration.getPageTitle());
        if (altairConfiguration.getTemplateParameters() != null) {
            altairConfiguration.getTemplateParameters().forEach((name, value) ->
                // De-capitalize and de-hyphenate the
                // parameter names.
                // Otherwise `graphiql.template-parameters
                // .magicWord` would be put as `magic-word`
                // in the parameters map
                // as Micronaut normalises properties and
                // stores them lowercase hyphen separated.
                parameters.put(NameUtils.decapitalize(
                    NameUtils.dehyphenate(name)), value));
        }
        return replaceParameters(this.rawTemplate, parameters);
    }

    private String replaceParameters(final String str, final Map<String, String> parameters) {
        Map<String, Object> map = new HashMap<>();
        map.putAll(parameters);
        PropertyResolver propertyResolver = new MapPropertyResolver(map);
        PropertyPlaceholderResolver propertyPlaceholderResolver = new DefaultPropertyPlaceholderResolver(
            propertyResolver);
        return propertyPlaceholderResolver.resolvePlaceholders(str).get();
    }
}
