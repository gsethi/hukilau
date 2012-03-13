package org.systemsbiology.cancerregulome.hukilau.configs;

import org.json.JSONObject;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.neo4j.kernel.HighlyAvailableGraphDatabase;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;
import org.systemsbiology.cancerregulome.hukilau.utils.DatabaseShutdownHookThread;

import java.util.HashMap;
import java.util.Iterator;
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
             AbstractGraphDatabase graphDb;

            JSONObject configMapping = mapping.JSON().getJSONObject("dbConfig");
            if(configMapping != null ){
                Map<String,String> dbConfigMap = new HashMap<String, String>();

                //create db configuration map
                Iterator iter = configMapping.keys();
                while(iter.hasNext()){
                     String key = (String) iter.next();
                     String value = configMapping.getString(key);
                     dbConfigMap.put(key,value);
                }

                if(configMapping.get("org.neo4j.server.database.mode") != null && ((String)configMapping.get("org.neo4j.server.database.mode")).equalsIgnoreCase("HA"))
                    graphDb = new HighlyAvailableGraphDatabase(location,dbConfigMap);
                else
                    graphDb = new EmbeddedReadOnlyGraphDatabase(location,dbConfigMap);
            }
            else
                graphDb = new EmbeddedReadOnlyGraphDatabase(location);

            getRuntime().addShutdownHook(new DatabaseShutdownHookThread(graphDb));
            addValue(mapping, graphDb);
        }
    }

}
