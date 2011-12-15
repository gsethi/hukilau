package org.systemsbiology.cancerregulome.hukilau.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Map;

/**
 * @author hrovira
 */
public class NetworkJsonUtils {
    public static JSONArray createNodeJSON(Map<Long, Node> nodeMap, Map<String, String> nodePropMap) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Node n : nodeMap.values()) {
            JSONObject json = new JSONObject();
            json.put("id", n.getId());
            for (String propKey : n.getPropertyKeys()) {
                json.put(propKey, n.getProperty(propKey));
                nodePropMap.put(propKey, propKey);
            }

            jsonArray.put(json);
        }
        return jsonArray;
    }

    public static JSONArray createEdgeJSON(Map<Long, Relationship> relMap, Map<String, String> relPropMap) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Relationship r : relMap.values()) {
            JSONObject json = new JSONObject();
            json.put("id", r.getId());
            json.put("source", r.getStartNode().getId());
            json.put("target", r.getEndNode().getId());

            for (String propKey : r.getPropertyKeys()) {
                json.put(propKey, r.getProperty(propKey));
                relPropMap.put(propKey, propKey);
            }

            jsonArray.put(json);
        }
        return jsonArray;
    }

    public static JSONArray createSchemaJSON(Map<String, String> nodePropMap) throws JSONException {
        //TODO: need to load into db different datatypes for properties and then retrieve them here appropriately
        JSONArray jsonArray = new JSONArray();
        for (String nProp : nodePropMap.values()) {
            JSONObject json = new JSONObject();
            json.put("name", nProp);
            json.put("type", "string");
            jsonArray.put(json);
        }
        return jsonArray;
    }
}
