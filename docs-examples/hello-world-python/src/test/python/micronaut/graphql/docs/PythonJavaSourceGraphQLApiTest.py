from typing import Annotated

from graphql import GraphQL
from jakarta.inject import Inject, Singleton
from java.util import Locale, Map
from micronaut.context.annotation import Factory, Property, Replaces, Requires
from micronaut.graphql import create_graphql
from micronaut.http import HttpRequest, HttpStatus
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .GraphQLFactory import GraphQLFactory


SCHEMA = """
type Query { locale(language: String!, country: String!): Locale }
type Locale { language: String country: String }
"""


def resolve_locale(source, info, *, language, country):
    return Locale(language, country)


@Factory
@Requires(property="spec.name", value="PythonJavaSourceGraphQLApiTest")
@Replaces(factory=GraphQLFactory)
class PythonJavaSourceGraphQLFactory:
    @Singleton
    def graph_ql(self) -> GraphQL:
        return create_graphql(SCHEMA, bindings={"Query": {"locale": resolve_locale}})


@MicronautTest
@Property(name="spec.name", value="PythonJavaSourceGraphQLApiTest")
class PythonJavaSourceGraphQLApiTest:
    """Java objects returned by Python resolvers are read with the schema's default data fetcher.

    A Java bean property is a getter, which a Python attribute lookup does not resolve.
    """

    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def test_java_object_fields_resolve_without_a_bean_context(self) -> None:
        graphql = create_graphql(SCHEMA, bindings={"Query": {"locale": resolve_locale}})
        result = graphql.execute('{ locale(language: "en", country: "US") { language country } }')
        assert result.getErrors().isEmpty(), str(result.getErrors())
        assert result.getData().get("locale").get("language") == "en"
        assert result.getData().get("locale").get("country") == "US"

    @Test
    def test_java_object_fields_resolve_through_the_graphql_bean(self) -> None:
        query = '{ locale(language: "fr", country: "CA") { language country } }'
        response = self.client.toBlocking().exchange(
            HttpRequest.POST("/graphql", {"query": query}), Map)
        assert response.status() == HttpStatus.OK
        body = response.body()
        assert not body.containsKey("errors"), str(body)
        assert body.get("data").get("locale").get("language") == "fr"
        assert body.get("data").get("locale").get("country") == "CA"
