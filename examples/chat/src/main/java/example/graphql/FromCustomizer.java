package example.graphql;

import graphql.ExecutionInput;
import io.micronaut.configuration.graphql.GraphQLExecutionInputCustomizer;
import io.micronaut.context.annotation.Primary;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;

@Singleton
@Primary
public class FromCustomizer implements GraphQLExecutionInputCustomizer {

    @SuppressWarnings("rawtypes")
    @Override
    public Publisher<ExecutionInput> customize(ExecutionInput executionInput, HttpRequest httpRequest) {
        return Publishers.just(executionInput.transform(
                builder -> builder.context(httpRequest.getRemoteAddress().toString())
        ));
    }
}
