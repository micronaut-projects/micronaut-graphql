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
package example.security;

import example.domain.User;
import example.repository.UserRepository;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.AuthenticationProvider;
import jakarta.inject.Singleton;

import java.util.Optional;

/**
 * @author Alexey Zhokhov
 */
@Singleton
public class AuthenticationProviderUserPassword implements AuthenticationProvider<HttpRequest<?>, String, String> {

    private final UserRepository userRepository;

    public AuthenticationProviderUserPassword(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public @NonNull AuthenticationResponse authenticate(@Nullable HttpRequest<?> requestContext, @NonNull AuthenticationRequest<String, String> authRequest) {
        Optional<User> user = userRepository.findByUsername(authRequest.getIdentity());

        if (user.isEmpty()) {
            return new AuthenticationFailed(AuthenticationFailureReason.USER_NOT_FOUND);
        }

        if (authRequest.getSecret().equals(user.get().getPassword())) {
            return AuthenticationResponse.success(user.get().getUsername(), user.get().getRoles());
        }

        return new AuthenticationFailed(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH);
    }
}
