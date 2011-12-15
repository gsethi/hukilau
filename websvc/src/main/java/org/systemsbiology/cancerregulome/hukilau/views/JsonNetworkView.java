package org.systemsbiology.cancerregulome.hukilau.views;

import org.json.JSONObject;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

import static org.systemsbiology.addama.commons.web.utils.HttpIO.getDesiredContentType;

/**
 * @author hrovira
 */
public class JsonNetworkView extends JsonView {
    @Override
    public void render(Map map, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject data = (JSONObject) map.get("data");
        if (data == null) data = new JSONObject();
        JSONObject dataSchema = (JSONObject) map.get("dataSchema");
        if (dataSchema == null) dataSchema = new JSONObject();

        JSONObject json = new JSONObject();
        json.put("data", data);
        json.put("dataSchema", dataSchema);
        response.setContentType(getDesiredContentType(request, this.getContentType()));

        PrintWriter writer = response.getWriter();
        writer.print(json.toString());
    }
}
