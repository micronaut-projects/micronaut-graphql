package example.micronaut;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "jackson.serializationInclusion", value = "ALWAYS")
@MicronautTest(startApplication = false)
class GraphQLJacksonSerializationTest {

    @Test
    void serializeGraphQLResponseBody(JsonMapper mapper) throws IOException {
        Map<String, Object> specification = Map.of("foo", "bar");
        var response = new GraphQLResponseBody(specification);
        var expected = """
            {"foo":"bar"}""";
        assertEquals(expected, mapper.writeValueAsString(response));
    }

    @Test
    void serializeEmptyGraphQLResponseBody(JsonMapper mapper) throws IOException {
        Map<String, Object> specification = Map.of("foo", Map.of("bar", Collections.emptyList()));
        var response = new GraphQLResponseBody(specification);
        var expected = """
            {"foo":{"bar":[]}}""";
        assertEquals(expected, mapper.writeValueAsString(response));
    }

    @Test
    void testRawJackson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String result = objectMapper.writeValueAsString(
            new GraphQLResponseBody(Map.of("data", "test")));
        assertEquals("""
            {"data":"test"}""", result);
    }

    @Test
    void testInjectedJackson() throws JsonProcessingException {
        try (ApplicationContext ctx = ApplicationContext.run()) {
            ObjectMapper objectMapper = ctx.getBean(ObjectMapper.class);
            String result = objectMapper.writeValueAsString(
                new GraphQLResponseBody(Map.of("data", "test")));
            assertEquals("""
            {"data":"test"}""", result);
        }
    }
}
