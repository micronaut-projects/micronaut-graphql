from typing import Annotated

from jakarta.inject import Inject
from micronaut.configuration.graphql import GraphiQLController
from micronaut.context import BeanContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@MicronautTest(startApplication=False)
class GraphiQLControllerTest:

    bean_context: Annotated[BeanContext, Inject]

    @Test
    def test_bean_of_type_graphiql_controller_exists(self) -> None:
        assert self.bean_context.containsBean(GraphiQLController)
