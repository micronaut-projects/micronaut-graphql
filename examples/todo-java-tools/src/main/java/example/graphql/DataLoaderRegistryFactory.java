package example.graphql;

import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.http.scope.RequestScope;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexey Zhokhov
 */
@Factory
public class DataLoaderRegistryFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DataLoaderRegistryFactory.class);

    private final AuthorDataLoader authorDataLoader;

    public DataLoaderRegistryFactory(AuthorDataLoader authorDataLoader) {
        this.authorDataLoader = authorDataLoader;
    }

    @SuppressWarnings("unused")
    @RequestScope
    public DataLoaderRegistry dataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        dataLoaderRegistry.register("author", DataLoader.newMappedDataLoader(authorDataLoader));
        LOG.trace("Created new data loader registry");
        return dataLoaderRegistry;
    }

}
