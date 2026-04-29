/*
 * Copyright 2017-2026 original authors
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

import graphql.schema.DataFetcherFactory;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.LightDataFetcher;
import graphql.schema.PropertyDataFetcher;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A GraphQL data fetcher that prefers Micronaut bean introspection before
 * falling back to GraphQL Java's reflective property lookup.
 *
 * @param <T> The fetched type
 * @since 1.0
 */
final class MicronautBeanPropertyDataFetcher<T> implements LightDataFetcher<T> {

    private static final MicronautBeanPropertyDataFetcher<Object> INSTANCE = new MicronautBeanPropertyDataFetcher<>();
    private static final DataFetcherFactory<Object> FACTORY = environment -> INSTANCE;

    private MicronautBeanPropertyDataFetcher() {
    }

    static DataFetcherFactory<Object> factory() {
        return FACTORY;
    }

    @Override
    public T get(GraphQLFieldDefinition fieldDefinition, Object source, Supplier<DataFetchingEnvironment> environmentSupplier) throws Exception {
        if (source == null) {
            return null;
        }
        String propertyName = fieldDefinition.getName();
        PropertyLookup propertyValue = readProperty(propertyName, source);
        if (propertyValue.resolved()) {
            return (T) propertyValue.value();
        }
        return (T) PropertyDataFetcher.fetching(propertyName).get(fieldDefinition, source, environmentSupplier);
    }

    @Override
    public T get(DataFetchingEnvironment environment) throws Exception {
        return get(environment.getFieldDefinition(), environment.getSource(), () -> environment);
    }

    private PropertyLookup readProperty(String propertyName, Object source) {
        if (source instanceof Map<?, ?> map) {
            return map.containsKey(propertyName) ? PropertyLookup.resolved(map.get(propertyName)) : PropertyLookup.unresolved();
        }
        return BeanIntrospector.SHARED.findIntrospection(source.getClass())
                .map(introspection -> readIntrospectedProperty(introspection, source, propertyName))
                .orElse(PropertyLookup.unresolved());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private PropertyLookup readIntrospectedProperty(BeanIntrospection introspection, Object source, String propertyName) {
        Object property = introspection.getProperty(propertyName).orElse(null);
        if (property == null) {
            return PropertyLookup.unresolved();
        }
        return PropertyLookup.resolved(((BeanProperty) property).get(source));
    }

    private record PropertyLookup(boolean resolved, Object value) {

        private static PropertyLookup resolved(Object value) {
            return new PropertyLookup(true, value);
        }

        private static PropertyLookup unresolved() {
            return new PropertyLookup(false, null);
        }
    }
}
