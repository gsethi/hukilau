package org.systemsbiology.cancerregulome.hukilau.configs;

import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;
import org.systemsbiology.cancerregulome.hukilau.utils.DatabaseShutdownHookThread;

import java.util.Map;

import static java.lang.Runtime.getRuntime;

/**
 * @author hrovira
 */
public class Neo4jGraphDbMappingsHandler extends MappingPropertyByIdContainer<AbstractGraphDatabase> implements MappingsHandler {
    public Neo4jGraphDbMappingsHandler(Map<String, AbstractGraphDatabase> map) {
        super(map, "location");
    }

    public void handle(Mapping mapping) throws Exception {
        if (jsonHasProperty(mapping)) {
            String location = mapping.JSON().getString("location");

            AbstractGraphDatabase graphDb = new EmbeddedReadOnlyGraphDatabase(location);
            getRuntime().addShutdownHook(new DatabaseShutdownHookThread(graphDb));
            addValue(mapping, graphDb);
        }
    }

}
