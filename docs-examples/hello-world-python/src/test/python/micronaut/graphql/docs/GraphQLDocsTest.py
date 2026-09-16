from typing import Annotated

import java
from jakarta.inject import Inject
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

Map = java.type("java.util.Map")


@MicronautTest
class GraphQLDocsTest:

    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def test_graphql_works_as_expected(self) -> None:
        get = HttpRequest.GET("graphql")
        get.getParameters().add("query", "query { hello }")

        retrieve = self.client.toBlocking().retrieve(get, Map)

        assert retrieve.get("data").get("hello") == "Hello World!"
