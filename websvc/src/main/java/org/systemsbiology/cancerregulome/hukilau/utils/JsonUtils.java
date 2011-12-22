package org.systemsbiology.cancerregulome.hukilau.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author hrovira
 */
public class JsonUtils {
    private static final Logger log = Logger.getLogger(JsonUtils.class.getName());

    public static JSONArray createNodeJSON(String baseUri, NodeMaps nodeMaps, String nodeLabel) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Node n : nodeMaps.getNodes()) {
            JSONObject json = new JSONObject();
            json.put("id", baseUri + "/nodes/" + n.getId());
            json.put("uri", baseUri + "/nodes/" + n.getId());
            for (String propKey : n.getPropertyKeys()) {
                json.put(propKey, n.getProperty(propKey));
            }
            if (!json.has("label")) {
                json.put("label", json.optString(nodeLabel));
            }

            jsonArray.put(json);
        }
        return jsonArray;
    }

    public static JSONArray createEdgeJSON(String baseUri, NodeMaps nodeMaps, String edgeLabel) throws JSONException {
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

            if (!json.has("label") && !isEmpty(edgeLabel)) {
                json.put("label", json.optString(edgeLabel));
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

    public static void addNumberOf(JSONObject json, String arrayKey) {
        try {
            if (!json.has(arrayKey)) {
                json.put(arrayKey, new JSONArray());
            }

            JSONArray items = json.getJSONArray(arrayKey);
            json.put("numberOf" + capitalize(arrayKey), items.length());
        } catch (JSONException e) {
            log.warning(e.getMessage());
        }

    }

    private static void appendCommon(Map<String, String> propertyMap, JSONArray jsonArray) throws JSONException {
        jsonArray.put(new JSONObject().put("name", "id").put("type", "string"));
        jsonArray.put(new JSONObject().put("name", "label").put("type", "string"));
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
