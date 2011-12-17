package org.systemsbiology.cancerregulome.hukilau.configs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.EmbeddedServerConfigurator;
import org.systemsbiology.addama.jsonconfig.JsonConfigHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.neo4j.server.configuration.Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY;
import static org.neo4j.server.configuration.Configurator.WEBSERVER_PORT_PROPERTY_KEY;

/**
 * @author hrovira
 */
public class Neo4jGraphJsonConfigHandler implements JsonConfigHandler {
    private static final Logger log = Logger.getLogger(Neo4jGraphJsonConfigHandler.class.getName());

    private final Map<String, EmbeddedGraphDatabase> graphDbsById;
    private final Map<String, Bootstrapper> bootstrapperById = new HashMap<String, Bootstrapper>();

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

                    if (item.has("webAdminPort") && item.has("webAdminAddress")) {
                        String webAdminPort = item.getString("webAdminPort");
                        String webAdminAddress = item.getString("webAdminAddress");

                        EmbeddedServerConfigurator config = new EmbeddedServerConfigurator(graphDb);
                        config.configuration().setProperty(WEBSERVER_PORT_PROPERTY_KEY, webAdminPort);
                        config.configuration().setProperty(WEBSERVER_ADDRESS_PROPERTY_KEY, webAdminAddress);

                        Bootstrapper neoServer = new WrappingNeoServerBootstrapper(graphDb, config);
                        neoServer.start();
                        bootstrapperById.put(id, neoServer);
                    }
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        for (Bootstrapper bootstrapper : bootstrapperById.values()) {
            try {
                bootstrapper.stop();
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
        }
        for (EmbeddedGraphDatabase graphDb : graphDbsById.values()) {
            try {
                graphDb.shutdown();
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
        }
        super.finalize();
    }
}
