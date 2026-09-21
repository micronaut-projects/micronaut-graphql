# tag::imports[]
from graphql.schema import DataFetcher, DataFetchingEnvironment
from jakarta.inject import Singleton
# end::imports[]


# tag::clazz[]
@Singleton
class HelloDataFetcher(DataFetcher[str]):

    def get(self, env: DataFetchingEnvironment) -> str:
        name = env.getArgument("name")
        if name is None or name.strip() == "":
            name = "World"
        return f"Hello {name}!"
# end::clazz[]
