package org.systemsbiology.cancerregulome.hukilau.configs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.EmbeddedServerConfigurator;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;

import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Runtime.getRuntime;
import static org.neo4j.server.configuration.Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY;
import static org.neo4j.server.configuration.Configurator.WEBSERVER_PORT_PROPERTY_KEY;

/**
 * @author hrovira
 */
public class Neo4jGraphJsonConfigHandler implements JsonConfigHandler {
    private static final Logger log = Logger.getLogger(Neo4jGraphJsonConfigHandler.class.getName());

    private final Map<String, EmbeddedGraphDatabase> graphDbsById;

    public Neo4jGraphJsonConfigHandler(Map<String, EmbeddedGraphDatabase> map) {
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

                    EmbeddedGraphDatabase graphDb = new EmbeddedGraphDatabase(location);
                    graphDbsById.put(id, graphDb);

                    Bootstrapper neoServer = newNeo4jBootstrapper(item, graphDb);
                    addShutdownHook(graphDb, neoServer);
                }
            }
        }
    }

    private Bootstrapper newNeo4jBootstrapper(JSONObject item, EmbeddedGraphDatabase graphDb) throws JSONException {
        if (item.has("webAdminPort") && item.has("webAdminAddress")) {
            Integer webAdminPort = item.getInt("webAdminPort");
            String webAdminAddress = item.getString("webAdminAddress");

            EmbeddedServerConfigurator config = new EmbeddedServerConfigurator(graphDb);
            config.configuration().setProperty(WEBSERVER_PORT_PROPERTY_KEY, webAdminPort);
            config.configuration().setProperty(WEBSERVER_ADDRESS_PROPERTY_KEY, webAdminAddress);

            Bootstrapper neoServer = new WrappingNeoServerBootstrapper(graphDb, config);
            neoServer.start();
            return neoServer;
        }
        return null;
    }

    private void addShutdownHook(final EmbeddedGraphDatabase graphDb, final Bootstrapper neoServer) {
        getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (neoServer != null) {
                        neoServer.stop();
                    }
                } catch (Exception e) {
                    log.warning(e.getMessage());
                }
                graphDb.shutdown();
            }
        });
    }

}
