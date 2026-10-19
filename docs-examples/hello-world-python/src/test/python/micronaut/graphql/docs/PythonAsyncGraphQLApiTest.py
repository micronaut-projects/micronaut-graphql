import asyncio
from dataclasses import dataclass
from typing import Annotated

from graphql import GraphQL
from jakarta.inject import Inject, Singleton
from java.util import Map
from micronaut.context.annotation import Factory, Property, Replaces, Requires
from micronaut.graphql import create_graphql
from micronaut.http import HttpRequest, HttpStatus
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .GraphQLFactory import GraphQLFactory


@dataclass
class Values:
    future: object
    custom: object


class AsyncResolvers:
    def __init__(self):
        self.ready = None

    async def first(self, source, info):
        self.ready = asyncio.Event()
        await asyncio.wait_for(self.ready.wait(), timeout=2)
        return "first"

    async def __call__(self, source, info):
        await asyncio.sleep(0.01)
        self.ready.set()
        return "second"

    async def values(self, source, info):
        await asyncio.sleep(0.01)
        loop = asyncio.get_running_loop()
        future = loop.create_future()
        loop.call_later(0.01, future.set_result, "future")

        class DelayedValue:
            def __await__(self):
                return asyncio.sleep(0.01, result="custom").__await__()

        return Values(future, DelayedValue())


@Factory
@Requires(property="spec.name", value="PythonAsyncGraphQLApiTest")
@Replaces(factory=GraphQLFactory)
class PythonAsyncGraphQLFactory:
    @Singleton
    def graph_ql(self) -> GraphQL:
        resolvers = AsyncResolvers()
        return create_graphql("""
            type Query { first: String second: String values: Values }
            type Values { future: String custom: String }
            """, bindings={"Query": {"first": resolvers.first, "second": resolvers,
                                     "values": resolvers.values}})


@MicronautTest
@Property(name="spec.name", value="PythonAsyncGraphQLApiTest")
class PythonAsyncGraphQLApiTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    def query(self, query: str) -> object:
        response = self.client.toBlocking().exchange(
            HttpRequest.POST("/graphql", {"query": query}), Map)
        assert response.status() == HttpStatus.OK
        body = response.body()
        assert not body.containsKey("errors"), str(body)
        return body.get("data")

    @Test
    def test_async_bound_and_callable_resolvers_suspend_and_progress_together(self) -> None:
        data = self.query("{ first second }")
        assert data.get("first") == "first"
        assert data.get("second") == "second"

    @Test
    def test_async_nested_values_accept_futures_and_custom_awaitables(self) -> None:
        data = self.query("{ values { future custom } }")
        assert data.get("values").get("future") == "future"
        assert data.get("values").get("custom") == "custom"
