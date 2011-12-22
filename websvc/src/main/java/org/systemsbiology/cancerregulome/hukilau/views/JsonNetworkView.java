package org.systemsbiology.cancerregulome.hukilau.views;


import org.json.JSONObject;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.cancerregulome.hukilau.pojo.NodeMaps;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.logging.Logger;

import static org.springframework.web.bind.ServletRequestUtils.getStringParameter;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getDesiredContentType;
import static org.systemsbiology.cancerregulome.hukilau.utils.JsonUtils.*;

/**
 * @author hrovira
 */
public class JsonNetworkView extends JsonView {
    private static final Logger log = Logger.getLogger(JsonNetworkView.class.getName());

    public static final String BASE_URI = "BASE_URI";
    public static final String NODE_MAPS = "NODE_MAPS";

    @Override
    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String baseUri = (String) map.get(BASE_URI);
        NodeMaps nodeMaps = (NodeMaps) map.get(NODE_MAPS);
        String nodeLabel = getStringParameter(request, "nodeLabel", "name");
        String edgeLabel = getStringParameter(request, "edgeLabel", "");

        log.info("baseUri: " + baseUri);
        log.info("nodes: " + nodeMaps.numberOfNodes());
        log.info("relationships: " + nodeMaps.numberOfRelationships());
        log.info("node label: " + nodeLabel);
        log.info("edge label: " + edgeLabel);

        JSONObject data = new JSONObject();
        data.put("nodes", createNodeJSON(baseUri, nodeMaps, nodeLabel));
        data.put("edges", createEdgeJSON(baseUri, nodeMaps, edgeLabel));
        addNumberOf(data, "nodes");
        addNumberOf(data, "edges");

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", nodeSchemaJSON(nodeMaps.getNodeProperties()));
        dataSchema.put("edges", edgeSchemaJSON(nodeMaps.getRelationshipProperties()));

        JSONObject json = new JSONObject().put("data", data).put("dataSchema", dataSchema);

        response.setContentType(getDesiredContentType(request, this.getContentType()));
        response.getWriter().print(json.toString());
    }
}
