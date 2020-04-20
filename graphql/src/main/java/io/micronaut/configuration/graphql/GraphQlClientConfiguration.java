package io.micronaut.configuration.graphql;

import java.util.Map;

public interface GraphQlClientConfiguration {


    /**
     * Returns the GraphiQL version.
     *
     * @return the GraphiQL version
     */
    String getVersion();

    /**
     * Returns the GraphQL client path.
     *
     * @return the GraphQL client path
     */
    String getPath();

    /**
     * Returns the GraphQl client template path.
     *
     * @return the Graphql client template path
     */
    String getTemplatePath();

    /**
     * Returns the GraphiQL template parameters to be substituted in the template.
     *
     * @return the GraphiQL template parameters
     */
    Map<String, String> getTemplateParameters();


    /**
     * Returns the GraphiQL page title.
     *
     * @return the GraphiQL page title
     */
    String getPageTitle();

}
