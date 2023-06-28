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
import io.micronaut.core.annotation.Nullable;

/**
 * @author Alexey Zhokhov
 */
public class LoginPayload {

    private final User user;
    private final String error;

    private LoginPayload(User user, String error) {
        this.user = user;
        this.error = error;
    }

    public static LoginPayload ofUser(User user) {
        return new LoginPayload(user, null);
    }

    public static LoginPayload ofError(String errorMessage) {
        return new LoginPayload(null, errorMessage);
    }

    @Nullable
    public User getUser() {
        return user;
    }

    @Nullable
    public String getError() {
        return error;
    }

}
