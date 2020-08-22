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
package io.micronaut.configuration.graphql;

import graphql.GraphQLContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.cookie.Cookie;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The helper class to work with {@link graphql.GraphQLContext} instance. The main purpose is to add ability to manage
 * cookies and headers inside GraphQL {@link graphql.schema.DataFetcher}. These cookies and headers will be propagated
 * to {@link GraphQLController}.
 *
 * @author Alexey Zhokhov
 * @since 1.4.0
 */
public final class GraphQLContextHttpUtils {

    private static final String HTTP_REQUEST = "MICRONAUT_HTTP_REQUEST";
    private static final String HTTP_RESPONSE_HEADERS = "MICRONAUT_HTTP_RESPONSE_HEADERS";
    private static final String HTTP_RESPONSE_COOKIES = "MICRONAUT_HTTP_RESPONSE_COOKIES";

    private GraphQLContextHttpUtils() {
    }

    /**
     * @param context The GraphQL context object
     * @return a HttpRequest from GraphQL context object or {@literal null} if no request bound to the context object
     */
    @Nullable
    public static HttpRequest getRequest(GraphQLContext context) {
        return context.get(HTTP_REQUEST);
    }

    /**
     * Set a response header.
     *
     * @param context The GraphQL context object
     * @param name    The name of the header
     * @param value   The value of the header
     */
    public static synchronized void setHeader(GraphQLContext context, String name, String value) {
        Map<String, String> headersBucket = context.getOrDefault(HTTP_RESPONSE_HEADERS, new LinkedHashMap<>());

        headersBucket.put(name, value);

        context.put(HTTP_RESPONSE_HEADERS, headersBucket);
    }

    /**
     * Add a cookie to the response.
     *
     * @param context The GraphQL context object
     * @param cookie  The cookie object
     */
    public static synchronized void addCookie(GraphQLContext context, Cookie cookie) {
        List<Cookie> cookiesBucket = context.getOrDefault(HTTP_RESPONSE_COOKIES, new ArrayList<>());

        cookiesBucket.add(cookie);

        context.put(HTTP_RESPONSE_COOKIES, cookiesBucket);
    }

    /**
     * Bind a http request to GraphQL context object.
     *
     * @param context The GraphQL context object
     * @param request The HTTP request
     */
    protected static synchronized void setRequest(GraphQLContext context, HttpRequest request) {
        context.put(HTTP_REQUEST, request);
    }

    /**
     * @param context The GraphQL context object
     * @return the map with headers, where the key is a header name and value is a header value
     */
    protected static Map<String, String> getHeaders(GraphQLContext context) {
        return context.getOrDefault(HTTP_RESPONSE_HEADERS, Collections.emptyMap());
    }

    /**
     * @param context The GraphQL context object
     * @return the list with cookie objects
     */
    protected static List<Cookie> getCookies(GraphQLContext context) {
        return context.getOrDefault(HTTP_RESPONSE_COOKIES, Collections.emptyList());
    }

}
