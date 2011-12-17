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
    public static JSONArray createNodeJSON(String baseUri, NodeMaps nodeMaps) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Node n : nodeMaps.getNodes()) {
            JSONObject json = new JSONObject();
            json.put("id", baseUri + "/nodes/" + n.getId());
            json.put("uri", baseUri + "/nodes/" + n.getId());
            for (String propKey : n.getPropertyKeys()) {
                json.put(propKey, n.getProperty(propKey));
            }

            jsonArray.put(json);
        }
        return jsonArray;
    }

    public static JSONArray createEdgeJSON(String baseUri, NodeMaps nodeMaps) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Relationship r : nodeMaps.getRelationships()) {
            JSONObject json = new JSONObject();
            json.put("id", baseUri + "/edges/" + r.getId());
            json.put("uri", baseUri + "/edges/" + r.getId());
            json.put("source", baseUri + "/nodes/" + r.getStartNode().getId());
            json.put("target", baseUri + "/nodes/" + r.getEndNode().getId());

            for (String propKey : r.getPropertyKeys()) {
                json.put(propKey, r.getProperty(propKey));
            }

            jsonArray.put(json);
        }
        return jsonArray;
    }

    public static JSONArray nodeSchemaJSON(Map<String, String> propertyMap) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        appendCommon(propertyMap, jsonArray);
        return jsonArray;
    }

    public static JSONArray edgeSchemaJSON(Map<String, String> propertyMap) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(new JSONObject().put("name", "source").put("type", "string"));
        jsonArray.put(new JSONObject().put("name", "target").put("type", "string"));
        appendCommon(propertyMap, jsonArray);
        return jsonArray;
    }

    private static void appendCommon(Map<String, String> propertyMap, JSONArray jsonArray) throws JSONException {
        jsonArray.put(new JSONObject().put("name", "id").put("type", "string"));
        jsonArray.put(new JSONObject().put("name", "uri").put("type", "string"));
        jsonArray.put(new JSONObject().put("name", "name").put("type", "string"));
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            JSONObject json = new JSONObject();
            json.put("name", entry.getKey());
            json.put("type", entry.getValue());
            jsonArray.put(json);
        }
    }
}
