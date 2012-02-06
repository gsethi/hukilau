package org.systemsbiology.cancerregulome.hukilau.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class NetworkConfiguration {
    private static final Logger log = Logger.getLogger(NetworkConfiguration.class.getName());

    private Map<String, Map<String, String>> nodeTypes = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, String>> edgeTypes = new HashMap<String, Map<String, String>>();
    private JSONArray nodeFiles = new JSONArray();
    private JSONArray edgeFiles = new JSONArray();

    private final JSONObject networkMetadata = new JSONObject();
    private Integer numberOfNodes = 0;
    private Integer numberOfEdges = 0;
    private String databaseRootPath;

    /*
     * Constructor
     */
    public NetworkConfiguration(JSONObject configuration) throws Exception {
        if (configuration.has("db")) {
            JSONObject dbObject = configuration.getJSONObject("db");
            this.databaseRootPath = dbObject.getString("rootPath");
            JSONArray edgeArray = dbObject.getJSONArray("edgeTypes");
            networkMetadata.put("edgeTypes", edgeArray);
            for (int i = 0; i < edgeArray.length(); i++) {
                JSONObject edgeTypeObject = (JSONObject) edgeArray.get(i);
                String typeName = edgeTypeObject.getString("name");
                Map<String, String> edgeProps = new HashMap<String, String>();
                if (edgeTypeObject.has("items")) {
                    JSONArray propItems = edgeTypeObject.getJSONArray("items");
                    for (int j = 0; j < propItems.length(); j++) {
                        JSONObject propItem = (JSONObject) propItems.get(j);
                        edgeProps.put(propItem.getString("name"), propItem.getString("type"));
                    }
                }

                edgeTypes.put(typeName, edgeProps);
            }
            JSONArray nodeArray = dbObject.getJSONArray("nodeTypes");
            networkMetadata.put("nodeTypes", nodeArray);
            for (int i = 0; i < nodeArray.length(); i++) {
                JSONObject nodeTypeObject = (JSONObject) nodeArray.get(i);
                String typeName = nodeTypeObject.getString("name");
                Map<String, String> nodeProps = new HashMap<String, String>();
                if (nodeTypeObject.has("items")) {
                    JSONArray propItems = nodeTypeObject.getJSONArray("items");
                    for (int j = 0; j < propItems.length(); j++) {
                        JSONObject propItem = (JSONObject) propItems.get(j);
                        nodeProps.put(propItem.getString("name"), propItem.getString("type"));
                    }
                }

                nodeTypes.put(typeName, nodeProps);
            }

        }
        if (configuration.has("nodeFiles")) {
            nodeFiles = configuration.getJSONArray("nodeFiles");
        }
        if (configuration.has("edgeFiles")) {
            edgeFiles = configuration.getJSONArray("edgeFiles");
        }
    }

    /*
     * Public Methods
     */
    public void incrementNodes() {
        numberOfNodes++;
    }

    public void incrementEdges() {
        numberOfEdges++;
    }

    public void outputNetworkMetadata() throws IOException, JSONException {
        networkMetadata.put("numberOfNodes", numberOfNodes);
        networkMetadata.put("numberOfEdges", numberOfEdges);

        File f = new File(databaseRootPath + "/network_metadata.json");
        if (!f.createNewFile()) {
            log.warning("unable to write network_metadata.json");
//            return;
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        out.write(networkMetadata.toString());
        out.flush();
        out.close();
    }

    public String getDatabaseRootPath() {
        return databaseRootPath;
    }

    public Map<String, Map<String, String>> getNodeTypes() {
        return nodeTypes;
    }

    public Map<String, Map<String, String>> getEdgeTypes() {
        return edgeTypes;
    }

    public JSONArray getNodeFiles() {
        return nodeFiles;
    }

    public JSONArray getEdgeFiles() {
        return edgeFiles;
    }
}
