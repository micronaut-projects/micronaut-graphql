# Copyright 2026 original authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Build a Micronaut GraphQL bean from SDL and Python resolvers."""

from __future__ import annotations

from collections.abc import Callable, Mapping
from dataclasses import dataclass
import inspect

import java

_GraphQL = java.type("graphql.GraphQL")
_SchemaParser = java.type("graphql.schema.idl.SchemaParser")
_SchemaGenerator = java.type("graphql.schema.idl.SchemaGenerator")
_RuntimeWiring = java.type("graphql.schema.idl.RuntimeWiring")
_TypeRuntimeWiring = java.type("graphql.schema.idl.TypeRuntimeWiring")
_ObjectTypeDefinition = java.type("graphql.language.ObjectTypeDefinition")
_Map = java.type("java.util.Map")
_List = java.type("java.util.List")
_AsyncioRuntime = java.type("io.micronaut.context.python.PythonAsyncioRuntime")

__all__ = ["ResolverInfo", "create_graphql"]


@dataclass(frozen=True)
class ResolverInfo:
    """Resolver metadata; context is a Python snapshot of the GraphQL context."""

    context: dict[object, object]


def _python_value(value):
    if isinstance(value, _Map):
        return {entry.getKey(): _python_value(entry.getValue()) for entry in value.entrySet()}
    if isinstance(value, _List):
        return [_python_value(item) for item in value]
    return value


def _attribute(source, name):
    if isinstance(source, (Mapping, _Map)):
        return source.get(name)
    return getattr(source, name, None)


async def _await_result(result):
    return await result


def _resolver_result(result):
    if inspect.isasyncgen(result):
        raise TypeError("Async generator GraphQL resolvers are not supported")
    if inspect.isawaitable(result):
        return _AsyncioRuntime.toCompletionStage(_await_result(result))
    return result


def _default_fetcher(environment):
    return _resolver_result(_attribute(environment.getSource(), environment.getFieldDefinition().getName()))


def _resolver_fetcher(resolver):
    def fetch(environment):
        context = {entry.getKey(): _python_value(entry.getValue())
                   for entry in environment.getGraphQlContext().stream().toList()}
        result = resolver(environment.getSource(), ResolverInfo(context),
                          **_python_value(environment.getArguments()))
        return _resolver_result(result)

    return fetch


def _attribute_fetcher(name):
    return lambda environment: _resolver_result(_attribute(environment.getSource(), name))


def create_graphql(sdl: str, *, bindings: Mapping[str, Mapping[str, Callable[..., object] | str]]) -> _GraphQL:
    """Create a real GraphQL bean using GraphQL Java's SDL parser and execution engine.

    Bindings map SDL type and field names to ``(source, info, **arguments)``
    callables (including bound methods), or Python attribute names. Unbound fields use
    their SDL name as an attribute or mapping key. Arguments, including nested input
    objects and lists, are ordinary Python values; omitted arguments keep Python defaults.
    SDL parsing and schema validation errors propagate unchanged.

    Async callables and awaitable results use Micronaut's asyncio runtime and complete
    through GraphQL Java's CompletionStage support. Async generators are not supported.
    ``info.context`` is a snapshot, not a mutable shared GraphQL context.
    """
    registry = _SchemaParser().parse(sdl)
    if not isinstance(bindings, Mapping):
        raise TypeError("bindings must map SDL type names to field bindings")
    object_types = {definition.getName(): definition
                    for definition in registry.getTypes(_ObjectTypeDefinition)}
    for type_name, fields in bindings.items():
        if type_name not in object_types:
            raise ValueError(f"Unknown GraphQL object type: {type_name}")
        if not isinstance(fields, Mapping):
            raise TypeError(f"Bindings for {type_name} must map field names to resolvers or attributes")
        definitions = [object_types[type_name],
                       *registry.objectTypeExtensions().getOrDefault(type_name, [])]
        field_names = {field.getName() for definition in definitions
                       for field in definition.getFieldDefinitions()}
        for field_name, binding in fields.items():
            if field_name not in field_names:
                raise ValueError(f"Unknown GraphQL field: {type_name}.{field_name}")
            if isinstance(binding, str):
                if not binding:
                    raise ValueError(f"Attribute name for {type_name}.{field_name} must not be empty")
            elif not callable(binding):
                raise TypeError(f"Binding for {type_name}.{field_name} must be a resolver or attribute name")
            elif (inspect.isasyncgenfunction(binding)
                  or inspect.isasyncgenfunction(getattr(binding, "__call__", None))):
                raise TypeError("Async generator GraphQL resolvers are not supported")

    wiring = _RuntimeWiring.newRuntimeWiring()
    for type_name in object_types:
        type_wiring = _TypeRuntimeWiring.newTypeWiring(type_name).defaultDataFetcher(_default_fetcher)
        for field_name, binding in bindings.get(type_name, {}).items():
            fetcher = _attribute_fetcher(binding) if isinstance(binding, str) else _resolver_fetcher(binding)
            type_wiring.dataFetcher(field_name, fetcher)
        wiring.type(type_wiring)
    schema = _SchemaGenerator().makeExecutableSchema(registry, wiring.build())
    return _GraphQL.newGraphQL(schema).build()
