package io.micronaut.configuration.graphql.altair;


import io.micronaut.configuration.graphql.GraphQLConfiguration;
import io.micronaut.configuration.graphql.GraphQlClientConfiguration;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Collections;
import java.util.Map;

@ConfigurationProperties(AltairConfiguration.PREFIX)
public class AltairConfiguration implements Toggleable, GraphQlClientConfiguration {
    /**
     * The prefix to use for all altair configuration properties.
     */
    public static final String PREFIX = "altair";

    /**
     * The default GraphiQL version.
     */
    public static final String DEFAULT_VERSION = "2.4.6";

    /**
     * The default enabled value.
     */
    public static final boolean DEFAULT_ENABLED = false;

    /**
     * The default GraphiQL path.
     */
    public static final String DEFAULT_PATH = "/altair";

    /**
     * The default GraphiQL template parameters.
     */
    public static final Map<String, String> DEFAULT_TEMPLATE_PARAMETERS = Collections.EMPTY_MAP;


    /**
     * The default GraphiQL template path.
     */
    public static final String DEFAULT_TEMPLATE_PATH = "classpath:altair/index.html";

    /**
     * The default GraphiQL page title.
     */
    public static final String DEFAULT_PAGE_TITLE = "altair";


    protected boolean enabled = DEFAULT_ENABLED;
    protected String version = DEFAULT_VERSION;
    protected String path = DEFAULT_PATH;
    protected String templatePath = DEFAULT_TEMPLATE_PATH;
    protected Map<String, String> templateParameters = DEFAULT_TEMPLATE_PARAMETERS;
    protected String pageTitle = DEFAULT_PAGE_TITLE;

    /**
     * The configuration name whether GraphiQL is enabled.
     */
    public static final String ENABLED = AltairConfiguration.PREFIX + ".enabled";

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the Altair path.
     *
     * @return the Altair path
     */
    public String getPath() {
        return path;
    }

    @Override
    public String getTemplatePath() {
        return templatePath;
    }

    @Override
    public Map<String, String> getTemplateParameters() {
        return templateParameters;
    }

    @Override
    public String getPageTitle() {
        return pageTitle;
    }
}

