package org.systemsbiology.cancerregulome.hukilau.utils;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.systemsbiology.addama.commons.web.exceptions.InvalidSyntaxException;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FilterUtils {
    private static final Logger log = Logger.getLogger(FilterUtils.class.getName());

    enum FilterType {numericrange}

    public static BooleanQuery buildBooleanQuery(JSONArray filter_list) throws JSONException {
        BooleanQuery bq = new BooleanQuery();

        for (int i = 0; i < filter_list.length(); i++) {
            JSONObject filter = filter_list.getJSONObject(i);

            FilterType type = getFilterType(filter);
            Query query = buildQuery(type, filter);
            if (query != null) {
                bq.add(new BooleanClause(query, BooleanClause.Occur.MUST));
            }
            else {
                log.warning("Unknown filter type: " + filter.toString(2));
            }
        }

        return bq;
    }

    public static FilterType getFilterType(JSONObject filter) throws JSONException {

        String filter_type = filter.getString("filter_type");
        return FilterType.valueOf(filter_type);
    }

    public static Query buildQuery(FilterType type, JSONObject filter) throws JSONException {
        switch (type) {
            case numericrange:
                String property = filter.getString("property");
                Double min = null;
                Double max = null;

                if (filter.has("min")) {
                    min = new Double(filter.getDouble("min"));
                }
                if (filter.has("max")) {
                    max = new Double(filter.getDouble("max"));
                }

                return NumericRangeQuery.newDoubleRange(property, min, max, true, true);
        }

        return null;
    }

    public static Map<String, ArrayList<JSONObject>> buildFilterMap(JSONArray filter_list) throws JSONException {
        Map<String, ArrayList<JSONObject>> filter_map = new HashMap<String, ArrayList<JSONObject>>();

        for (int i = 0; i < filter_list.length(); i++) {
            JSONObject filter = filter_list.getJSONObject(i);

            String property = filter.getString("property");

            if (!filter_map.containsKey(property)) {
                filter_map.put(property, new ArrayList<JSONObject>());
            }

            filter_map.get(property).add(filter);
        }

        return filter_map;
    }

    public static boolean applyFilters(PropertyContainer container, Map<String, ArrayList<JSONObject>> filter_map) throws JSONException {

        for (String propKey : container.getPropertyKeys()) {
            if (filter_map.containsKey(propKey)) {
                ArrayList<JSONObject> prop_filters = filter_map.get(propKey);

                for (JSONObject filter : prop_filters) {
                    FilterType type = getFilterType(filter);

                    switch (type) {
                        case numericrange:
                            Double property = (Double)container.getProperty(propKey);
                            Double min = Double.MIN_VALUE;
                            Double max = Double.MAX_VALUE;

                            if (filter.has("min")) {
                                min = new Double(filter.getDouble("min"));
                            }
                            if (filter.has("max")) {
                                max = new Double(filter.getDouble("max"));
                            }

                            if (!(min <= property && property <= max)) {
                                return false;
                            }
                    }
                }
            }
        }

        return true;
    }

    public static ArrayList<Node> applyNodeFilters(NodeMaps container, JSONArray filter_list) throws JSONException {
        Map<String, ArrayList<JSONObject>> filter_map = buildFilterMap(filter_list);

        ArrayList<Node> filtered = new ArrayList<Node>();

        for (Node node : container.getNodes()) {
            if (applyFilters(node, filter_map)) {
                filtered.add(node);
            }
        }

        return filtered;
    }

    public static ArrayList<Relationship> applyEdgeFilters(NodeMaps container, JSONArray filter_list) throws JSONException {
        Map<String, ArrayList<JSONObject>> filter_map = buildFilterMap(filter_list);

        ArrayList<Relationship> filtered = new ArrayList<Relationship>();

        for (Relationship rel : container.getRelationships()) {
            if (applyFilters(rel, filter_map)) {
                filtered.add(rel);
            }
        }

        return filtered;
    }

    public static NodeMaps filterNodeMaps(NodeMaps original, JSONArray node_filters, JSONArray edge_filters) throws JSONException {
        ArrayList<Node> filtered_nodes = FilterUtils.applyNodeFilters(original, node_filters);
        ArrayList<Relationship> filtered_edges = FilterUtils.applyEdgeFilters(original, edge_filters);

        NodeMaps nodeMaps = new NodeMaps();

        for (Node node : filtered_nodes) {
            nodeMaps.addNode(node);
        }

        for (Relationship rel : filtered_edges) {
            nodeMaps.addRelationship(rel);
        }

        nodeMaps.tieUpLooseEnds();
        return nodeMaps;
    }
}
