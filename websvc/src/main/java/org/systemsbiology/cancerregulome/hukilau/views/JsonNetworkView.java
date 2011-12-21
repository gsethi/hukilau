package org.systemsbiology.cancerregulome.hukilau.views;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.systemsbiology.addama.commons.web.utils.HttpIO.getDesiredContentType;

/**
 * @author hrovira
 */
public class JsonNetworkView extends JsonView {
    private static final Logger log = Logger.getLogger(JsonNetworkView.class.getName());

    @Override
    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject data = (JSONObject) map.get("data");
        if (data == null) data = new JSONObject();
        addNumberOf(data, "nodes");
        addNumberOf(data, "edges");

        JSONObject dataSchema = (JSONObject) map.get("dataSchema");
        if (dataSchema == null) dataSchema = new JSONObject();

        JSONObject json = new JSONObject();
        json.put("data", data);
        json.put("dataSchema", dataSchema);
        response.setContentType(getDesiredContentType(request, this.getContentType()));

        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
    }

    private void addNumberOf(JSONObject data, String arrayKey) {
        try {
            if (!data.has(arrayKey)) {
                data.put(arrayKey, new JSONArray());
            }

            JSONArray items = data.getJSONArray(arrayKey);
            data.put("numberOf" + capitalize(arrayKey), items.length());
        } catch (JSONException e) {
            log.warning(e.getMessage());
        }

    }
}
