package example.micronaut;

import io.micronaut.configuration.graphql.GraphQLResponseBody;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class GraphQLJacksonSerializationTest {

    @Test
    void serializeGraphQLResponseBody(JsonMapper mapper) throws IOException {
        Map<String, Object> specification = Collections.singletonMap("foo", "bar");
        var response = new GraphQLResponseBody(specification);
        var expected = "{\"foo\":\"bar\"}";
        assertEquals(expected, mapper.writeValueAsString(response));
    }
}
