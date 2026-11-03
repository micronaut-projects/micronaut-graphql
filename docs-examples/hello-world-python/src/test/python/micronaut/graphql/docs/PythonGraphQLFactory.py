from dataclasses import dataclass

from graphql import GraphQL
from jakarta.inject import Singleton
from micronaut.context.annotation import Factory, Replaces, Requires
from micronaut.graphql import ResolverInfo, create_graphql

from .GraphQLFactory import GraphQLFactory


SCHEMA = """
type Query { bookById(id: ID): Book }
type Book { id: ID name: String pageCount: Int author: Author }
type Author { firstName: String lastName: String }
"""


@dataclass
class Author:
    first_name: str
    last_name: str


@dataclass
class Book:
    id: str
    name: str
    page_count: int
    author: Author


@Singleton
@Requires(property="spec.name", value="PythonGraphQLApiTest")
class BookResolvers:
    def book_by_id(self, source: object, info: ResolverInfo, id: str | None = None) -> Book | None:
        assert source is None
        assert id is None or isinstance(id, str)
        if id != "book-1":
            return None
        return Book("book-1", "Harry Potter and the Philosopher's Stone", 223,
                    Author("Joanne", "Rowling"))

    def author(self, source: Book, info: ResolverInfo) -> Author:
        assert source.id == "book-1"
        assert source.page_count == 223
        return source.author


@Factory
@Requires(property="spec.name", value="PythonGraphQLApiTest")
@Replaces(factory=GraphQLFactory)
class PythonGraphQLFactory:
    # tag::python-factory-method[]
    @Singleton
    def graph_ql(self, resolvers: BookResolvers) -> GraphQL:
        return create_graphql(SCHEMA, bindings={
            "Query": {"bookById": resolvers.book_by_id},
            "Book": {"author": resolvers.author, "pageCount": "page_count"},
            "Author": {"firstName": "first_name", "lastName": "last_name"},
        })
    # end::python-factory-method[]
