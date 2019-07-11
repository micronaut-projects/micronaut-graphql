/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.graphql.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.type.Argument;
import io.micronaut.graphql.annotations.GraphQLFieldName;
import io.micronaut.graphql.annotations.GraphQLTypeMapping;
import io.micronaut.graphql.annotations.GraphQLTypeName;
import io.micronaut.graphql.annotations.GraphQLTypeResolver;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.dataloader.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Gathers different GraphQL type annotation mappings with {@link GraphQLTypeMapping} and contributes to creation
 * of {@link RuntimeWiring} using {@link RuntimeWiring.Builder}.
 *
 * @author Denis Stepanov
 * @since 1.3
 */
@Singleton
public class AnnotatedMethodRuntimeWiringBuilder implements ExecutableMethodProcessor<GraphQLTypeMapping>, GraphQLRuntimeWiringBuilderCustomizer {

    private final Logger log = LoggerFactory.getLogger(AnnotatedMethodRuntimeWiringBuilder.class);

    private final Map<String, List<Consumer<TypeRuntimeWiring.Builder>>> typeMappings = new HashMap<>();

    private final ObjectMapper objectMapper;
    private final BeanContext beanContext;

    /**
     * @param objectMapper ObjectMapper to deserialize incoming values
     * @param beanContext BeanContext to find instances to be invoked from context
     */
    public AnnotatedMethodRuntimeWiringBuilder(ObjectMapper objectMapper, BeanContext beanContext) {
        this.objectMapper = objectMapper;
        this.beanContext = beanContext;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        String typeName = method.getValue(GraphQLTypeName.class, String.class)
                .orElseGet(() -> beanDefinition.getValue(GraphQLTypeName.class, String.class).get());

        typeMappings.computeIfAbsent(typeName, (key) -> new LinkedList<>())
                .add(builder -> mapType(typeName, builder, beanDefinition, method));
    }

    @Override
    public void accept(RuntimeWiring.Builder runtimeBuilder) {
        typeMappings.forEach((typeName, typeMapping) -> {
            TypeRuntimeWiring.Builder typeBuilder = new TypeRuntimeWiring.Builder();
            typeBuilder.typeName(typeName);
            typeMapping.forEach(builderConsumer -> builderConsumer.accept(typeBuilder));
            runtimeBuilder.type(typeBuilder.build());
        });
    }

    private void mapType(String typeName, TypeRuntimeWiring.Builder builder, BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        Class<? extends Annotation> annotation = method.getAnnotationTypeByStereotype(GraphQLTypeMapping.class)
                .orElseThrow(() -> new IllegalStateException("Cannot find GraphQLTypeMapping"));
        if (annotation.equals(GraphQLTypeResolver.class)) {
            if (!TypeResolver.class.isAssignableFrom(beanDefinition.getBeanType())) {
                throw new IllegalStateException("Bean needs to be an instance of 'graphql.schema.TypeResolver'");
            }
            mapTypeResolver(typeName, builder, beanDefinition);
        } else {
            mapDataFetcher(typeName, builder, beanDefinition, method);
        }
    }

    private void mapTypeResolver(String typeName, TypeRuntimeWiring.Builder builder, BeanDefinition<?> beanDefinition) {
        log.debug("Mapping type resolver: {} for type: {}", beanDefinition, typeName);
        builder.typeResolver(getBeanInstance(beanDefinition));
    }

    private void mapDataFetcher(String typeName, TypeRuntimeWiring.Builder builder, BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        String fieldName = method.getValue(GraphQLFieldName.class, String.class).orElseGet(() -> method.getMethodName());
        log.debug("Mapping data fetcher: {} {} for type: {} and field: {}", beanDefinition, method, typeName, fieldName);
        DataFetcher dataFetcher;
        // Addition check if mapping DataFetcher's 'get' method
        if (DataFetcher.class.isAssignableFrom(beanDefinition.getBeanType()) && "get".equals(method.getMethodName())) {
            dataFetcher = getBeanInstance(beanDefinition);
        } else {
            dataFetcher = environment -> {
                Object[] invokeValues = resolveArgumentValues(environment, method.getArguments(), method.toString());
                Object beanInstance = getBeanInstance(beanDefinition);
                return ((ExecutableMethod) method).invoke(beanInstance, invokeValues);
            };
        }
        builder.dataFetcher(fieldName, dataFetcher);
    }

    private <T> T getBeanInstance(BeanDefinition<?> beanDefinition) {
        io.micronaut.context.Qualifier<T> qualifer = (io.micronaut.context.Qualifier<T>) beanDefinition
                .getAnnotationTypeByStereotype(Qualifier.class)
                .map(type -> Qualifiers.byAnnotation(beanDefinition, type))
                .orElse(null);
        Class<T> beanType = (Class<T>) beanDefinition.getBeanType();
        return beanContext.getBean(beanType, qualifer);
    }

    private Object[] resolveArgumentValues(DataFetchingEnvironment environment, Argument[] arguments, String logMethodSignature) {
        return Stream.of(arguments)
                .map(argument -> resolveArgumentValue(environment, argument, logMethodSignature))
                .toArray();
    }

    private Object resolveArgumentValue(DataFetchingEnvironment environment, Argument argument, String logMethodSignature) {
        if (argument.getType().isInstance(environment)) {
            return environment;
        } else if (environment.getSource() != null && argument.getType().isInstance(environment.getSource())) {
            return environment.getSource();
        } else if (DataLoader.class.isAssignableFrom(argument.getType())) {
            return environment.getDataLoader(argument.getName());
        }
        Object argumentValue = environment.getArgument(argument.getName());
        if (argumentValue == null) {
            throw new IllegalStateException("Cannot find a value for argument: " + argument.getName() + " " + logMethodSignature);
        }
        return objectMapper.convertValue(argumentValue, argument.getType());
    }

}
