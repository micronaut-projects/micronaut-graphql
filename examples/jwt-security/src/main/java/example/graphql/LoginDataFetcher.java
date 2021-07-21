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
package example.graphql;

import example.domain.User;
import example.repository.UserRepository;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.CookieConfiguration;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.Authenticator;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.event.LoginFailedEvent;
import io.micronaut.security.event.LoginSuccessfulEvent;
import io.micronaut.security.token.jwt.generator.AccessRefreshTokenGenerator;
import io.micronaut.security.token.jwt.generator.AccessTokenConfiguration;
import io.micronaut.security.token.jwt.render.AccessRefreshToken;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;

import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.Random;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class LoginDataFetcher implements DataFetcher<LoginPayload> {

    private static final int LOGIN_RATE_LIMIT = 10;
    private static int LOGIN_RATE_LIMIT_REMAINING = LOGIN_RATE_LIMIT;

    private final Authenticator authenticator;
    private final ApplicationEventPublisher eventPublisher;
    private final CookieConfiguration cookieConfiguration;
    private final AccessRefreshTokenGenerator accessRefreshTokenGenerator;
    private final AccessTokenConfiguration accessTokenConfiguration;

    private final UserRepository userRepository;

    public LoginDataFetcher(Authenticator authenticator,
                            ApplicationEventPublisher eventPublisher,
                            CookieConfiguration cookieConfiguration,
                            AccessRefreshTokenGenerator accessRefreshTokenGenerator,
                            AccessTokenConfiguration accessTokenConfiguration, UserRepository userRepository) {
        this.authenticator = authenticator;
        this.eventPublisher = eventPublisher;
        this.cookieConfiguration = cookieConfiguration;
        this.accessRefreshTokenGenerator = accessRefreshTokenGenerator;
        this.accessTokenConfiguration = accessTokenConfiguration;
        this.userRepository = userRepository;
    }

    @Override
    public LoginPayload get(DataFetchingEnvironment environment) throws Exception {
        GraphQLContext graphQLContext = environment.getContext();

        if (LOGIN_RATE_LIMIT_REMAINING <= 0) {
            addRateLimitHeaders(graphQLContext);

            resetRateLimit();

            return LoginPayload.ofError("Rate Limit Exceeded");
        }

        HttpRequest httpRequest = graphQLContext.get("httpRequest");
        MutableHttpResponse<String> httpResponse = graphQLContext.get("httpResponse");

        String username = environment.getArgument("username");
        String password = environment.getArgument("password");

        UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(username, password);

        LOGIN_RATE_LIMIT_REMAINING--;

        Flux<AuthenticationResponse> authenticationResponseFlowable =
                Flux.from(authenticator.authenticate(httpRequest, usernamePasswordCredentials));

        return authenticationResponseFlowable.map(authenticationResponse -> {
            addRateLimitHeaders(graphQLContext);

            if (authenticationResponse.isAuthenticated()) {
                UserDetails userDetails = (UserDetails) authenticationResponse;
                eventPublisher.publishEvent(new LoginSuccessfulEvent(userDetails));

                Optional<Cookie> jwtCookie = accessTokenCookie(userDetails, httpRequest);
                jwtCookie.ifPresent(httpResponse::cookie);

                User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);

                return LoginPayload.ofUser(user);
            } else {
                eventPublisher.publishEvent(new LoginFailedEvent(authenticationResponse));

                return LoginPayload.ofError(authenticationResponse.getMessage().orElse(null));
            }
        }).blockFirst();
    }

    private Optional<Cookie> accessTokenCookie(UserDetails userDetails, HttpRequest<?> request) {
        Optional<AccessRefreshToken> accessRefreshTokenOptional = accessRefreshTokenGenerator.generate(userDetails);
        if (accessRefreshTokenOptional.isPresent()) {
            Cookie cookie = Cookie.of(cookieConfiguration.getCookieName(), accessRefreshTokenOptional.get().getAccessToken());
            cookie.configure(cookieConfiguration, request.isSecure());
            Optional<TemporalAmount> cookieMaxAge = cookieConfiguration.getCookieMaxAge();
            if (cookieMaxAge.isPresent()) {
                cookie.maxAge(cookieMaxAge.get());
            } else {
                cookie.maxAge(accessTokenConfiguration.getExpiration());
            }
            return Optional.of(cookie);
        }
        return Optional.empty();
    }

    private void addRateLimitHeaders(GraphQLContext graphQLContext) {
        MutableHttpResponse<String> httpResponse = graphQLContext.get("httpResponse");

        httpResponse.header("X-Login-RateLimit", String.valueOf(LOGIN_RATE_LIMIT));
        httpResponse.header("X-Login-RateLimit-Remaining", String.valueOf(LOGIN_RATE_LIMIT_REMAINING));
    }

    private void resetRateLimit() {
        int random = new Random().nextInt(5);
        if (random == 3) {
            LOGIN_RATE_LIMIT_REMAINING = LOGIN_RATE_LIMIT;
        }
    }

}
