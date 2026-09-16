# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples under `docs-examples/hello-world-python`
that are present but disabled, or that deviate from the Java example because the direct port does
not compile or does not behave like the Java example yet. It is the bug-fixing task list for
the Python compiler (`micronaut-inject-python` / `micronaut-context-python`); every row references a
`TODO(python)` comment in the sources or a workaround described below.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow).

## Reconciliation

- Last generated active `@Disabled` count: 0.
- Last generated command: `rg -n "@Disabled\(" docs-examples/hello-world-python/src`.
- Last full-suite command: `./gradlew :micronaut-docs-examples:micronaut-hello-world-python:test -Ppython-ci`.
- Last full-suite result: build successful, 2 tests executed, 0 skipped, 0 failures.

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs snippets.
  Standard Micronaut annotations are imported from their Java package (`micronaut.context.annotation`, `jakarta.inject`, ...).
- The GraphQL Java classes are imported from their Java packages (`from graphql import GraphQL`,
  `from graphql.schema import DataFetcher, DataFetchingEnvironment`, `from graphql.schema.idl import RuntimeWiring, ...`).
- `HelloDataFetcher` implements the generic Java interface `DataFetcher[str]`; the Python bean is injected into the
  `@Factory` method and passed to the GraphQL Java `dataFetcher(...)` builder call directly.
- The `RuntimeWiring` type wiring lambda of the Java example is a Python `lambda` (host interop converts it to the
  `UnaryOperator` parameter of `RuntimeWiring.Builder.type`).
- The tests are `@MicronautTest` classes injecting the beans as class attributes (`Annotated[HttpClient, Inject, Client("/")]`).

## Active `@Disabled` Tests

None.

## Commented Unsupported Snippet Ports

None.

## Workarounds Kept In Snippets

None.

## Intentionally Unsupported Snippet Targets

None.

## java.type usages

| File | Alias | Reason |
| --- | --- | --- |
| `GraphQLDocsTest.py` | `Map = java.type("java.util.Map")` | Runtime type argument of `BlockingHttpClient.retrieve(request, Map)`; imported shim classes only work as type hints. |
| `GraphiQLControllerTest.py` | `java.type("io.micronaut.configuration.graphql.GraphiQLController")` | Runtime type argument of `BeanContext.containsBean(...)`. |
