package org.systemsbiology.cancerregulome.hukilau.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import java.util.Map;

/**
 * @author hrovira
 */
public class JsonUtils {
    public static JSONArray createNodeJSON(NodeMaps nodeMaps) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Node n : nodeMaps.getNodes()) {
            JSONObject json = new JSONObject();
            json.put("id", n.getId());
            for (String propKey : n.getPropertyKeys()) {
                json.put(propKey, n.getProperty(propKey));
            }

            jsonArray.put(json);
        }
        return jsonArray;
    }

    public static JSONArray createEdgeJSON(NodeMaps nodeMaps) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Relationship r : nodeMaps.getRelationships()) {
            JSONObject json = new JSONObject();
            json.put("id", r.getId());
            json.put("source", r.getStartNode().getId());
            json.put("target", r.getEndNode().getId());

            for (String propKey : r.getPropertyKeys()) {
                json.put(propKey, r.getProperty(propKey));
            }

            jsonArray.put(json);
        }
        return jsonArray;
    }

    public static JSONArray createSchemaJSON(Map<String, String> propertyMap) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            JSONObject json = new JSONObject();
            json.put("name", entry.getKey());
            json.put("type", entry.getValue());
            jsonArray.put(json);
        }
        return jsonArray;
    }
}
