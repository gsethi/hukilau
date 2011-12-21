package org.systemsbiology.cancerregulome.hukilau.configs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;
import org.systemsbiology.cancerregulome.hukilau.utils.DatabaseShutdownHookThread;

import java.util.Map;

import static java.lang.Runtime.getRuntime;

/**
 * @author hrovira
 */
public class Neo4jGraphJsonConfigHandler implements JsonConfigHandler {
    private final Map<String, AbstractGraphDatabase> graphDbsById;

    public Neo4jGraphJsonConfigHandler(Map<String, AbstractGraphDatabase> map) {
        this.graphDbsById = map;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("locals")) {
            JSONArray locals = configuration.getJSONArray("locals");
            for (int i = 0; i < locals.length(); i++) {
                JSONObject item = locals.getJSONObject(i);
                if (item.has("id") && item.has("location")) {
                    String id = item.getString("id");
                    String location = item.getString("location");

                    AbstractGraphDatabase graphDb = new EmbeddedReadOnlyGraphDatabase(location);
                    graphDbsById.put(id, graphDb);
                    getRuntime().addShutdownHook(new DatabaseShutdownHookThread(graphDb));
                }
            }
        }
    }

}
