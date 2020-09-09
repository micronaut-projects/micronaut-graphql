package example.graphql;

import edu.umd.cs.findbugs.annotations.Nullable;
import graphql.ExecutionInput;
import graphql.GraphQLContext;
import io.micronaut.configuration.graphql.GraphQLExecutionInputCustomizer;
import io.micronaut.context.annotation.Primary;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;

@Singleton
@Primary
public class RequestResponseCustomizer implements GraphQLExecutionInputCustomizer {

    @Override
    public Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest,
                                               @Nullable MutableHttpResponse<String> httpResponse) {
        GraphQLContext graphQLContext = (GraphQLContext) executionInput.getContext();
        graphQLContext.put("httpRequest", httpRequest);
        graphQLContext.put("httpResponse", httpResponse);
        return Publishers.just(executionInput);
    }

}
