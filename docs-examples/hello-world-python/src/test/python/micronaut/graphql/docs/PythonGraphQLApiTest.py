from typing import Annotated

from graphql import ExecutionInput, GraphQL
from graphql.parser import InvalidSyntaxException
from graphql.schema.idl.errors import SchemaProblem
from jakarta.inject import Inject
from java.util import Map
from micronaut.context import BeanContext
from micronaut.context.annotation import Property
from micronaut.graphql import create_graphql
from micronaut.http import HttpRequest, HttpStatus
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@MicronautTest
@Property(name="spec.name", value="PythonGraphQLApiTest")
class PythonGraphQLApiTest:
    client: Annotated[HttpClient, Inject, Client("/")]
    bean_context: Annotated[BeanContext, Inject]

    def query(self, argument: str | None) -> object:
        arguments = "(" + argument + ")" if argument is not None else ""
        query = "{ bookById" + arguments + " { id name pageCount author { firstName lastName } } }"
        response = self.client.toBlocking().exchange(
            HttpRequest.POST("/graphql", {"query": query}), Map)
        assert response.status() == HttpStatus.OK
        body = response.body()
        assert not body.containsKey("errors"), str(body)
        return body.get("data").get("bookById")

    @Test
    def test_bound_resolvers_and_python_attribute_mappings_over_http(self) -> None:
        assert self.bean_context.containsBean(GraphQL)
        book = self.query('id: "book-1"')
        assert book.get("id") == "book-1"
        assert book.get("name") == "Harry Potter and the Philosopher's Stone"
        assert book.get("pageCount") == 223
        assert book.get("author").get("firstName") == "Joanne"
        assert book.get("author").get("lastName") == "Rowling"

    @Test
    def test_unknown_and_null_arguments_return_null(self) -> None:
        assert self.query('id: "missing"') is None
        assert self.query("id: null") is None
        assert self.query(None) is None

    @Test
    def test_resolver_receives_context_and_keyword_arguments(self) -> None:
        def resolve(source, info, *, name="World"):
            assert source is None
            return info.context["greeting"] + " " + name

        graphql = create_graphql("type Query { hello(name: String): String }",
                                 bindings={"Query": {"hello": resolve}})
        execution = (ExecutionInput.newExecutionInput().query('{ hello(name: "Python") }')
                     .graphQLContext({"greeting": "Hello"}).build())
        result = graphql.execute(execution)
        assert result.getErrors().isEmpty(), str(result.getErrors())
        assert result.getData().get("hello") == "Hello Python"
        execution = (ExecutionInput.newExecutionInput().query("{ hello }")
                     .graphQLContext({"greeting": "Hello"}).build())
        result = graphql.execute(execution)
        assert result.getErrors().isEmpty(), str(result.getErrors())
        assert result.getData().get("hello") == "Hello World"

    @Test
    def test_invalid_sdl_preserves_parser_and_schema_errors(self) -> None:
        try:
            create_graphql("type Query {", bindings={})
        except InvalidSyntaxException:
            pass
        else:
            assert False, "Invalid SDL must not be accepted"
        try:
            create_graphql("type Query { book: MissingType }", bindings={})
        except SchemaProblem:
            pass
        else:
            assert False, "Invalid schema must not be accepted"

    @Test
    def test_invalid_bindings_fail_before_execution(self) -> None:
        for bindings in ({"MissingType": {"hello": "hello"}},
                         {"Query": {"missingField": "hello"}},
                         {"Query": {"hello": ""}}):
            try:
                create_graphql("type Query { hello: String }", bindings=bindings)
            except ValueError:
                pass
            else:
                assert False, "Invalid binding must not be accepted"
        try:
            create_graphql("type Query { hello: String }", bindings={"Query": {"hello": 42}})
        except TypeError:
            pass
        else:
            assert False, "A binding must be a resolver or an attribute name"

    @Test
    def test_async_resolvers_are_rejected_explicitly(self) -> None:
        async def resolve(source, info):
            return "Hello"

        try:
            create_graphql("type Query { hello: String }", bindings={"Query": {"hello": resolve}})
        except TypeError as error:
            assert "async" in str(error).lower()
        else:
            assert False, "Async resolvers need an explicit supported bridge"

    @Test
    def test_synchronous_resolver_cannot_silently_return_an_awaitable(self) -> None:
        class AwaitableResult:
            def __await__(self):
                yield
                return "Hello"

        def resolve(source, info):
            return AwaitableResult()

        graphql = create_graphql("type Query { hello: String }", bindings={"Query": {"hello": resolve}})
        result = graphql.execute("{ hello }")
        assert result.getData().get("hello") is None
        assert result.getErrors().size() == 1
        assert "async" in result.getErrors().get(0).getMessage().lower()
