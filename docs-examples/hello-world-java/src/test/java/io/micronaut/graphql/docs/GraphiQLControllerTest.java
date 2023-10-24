package io.micronaut.graphql.docs;

import io.micronaut.configuration.graphql.GraphiQLController;
import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class GraphiQLControllerTest {

    @Inject
    BeanContext beanContext;

    @Test
    void beanOfTypeGraphiQLControllerExists() {
        assertTrue(beanContext.containsBean(GraphiQLController.class));
    }
}
