package org.systemsbiology.cancerregulome.hukilau.rest;

import org.json.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.systemsbiology.addama.commons.web.views.JsonView;

import javax.servlet.http.HttpServletRequest;

import java.util.logging.Logger;

/**
 * @author aeakin
 */
@Controller
public class QueryController  {
    private static final Logger log = Logger.getLogger(QueryController.class.getName());

    @RequestMapping(value = "/**", method = RequestMethod.GET)
    protected ModelAndView handleGraphRetrieval(HttpServletRequest request) throws Exception {
        String requestUri = request.getRequestURI();
        log.info(requestUri);

        JSONObject data = new JSONObject();
        data.put("nodes", new JSONArray());
        data.put("edges", new JSONArray());

        JSONObject dataSchema = new JSONObject();
        dataSchema.put("nodes", new JSONArray());
        dataSchema.put("edges", new JSONArray());

        JSONObject json = new JSONObject();
        json.put("data", data);
        json.put("dataSchema", dataSchema);
        return new ModelAndView(new JsonView()).addObject("json", json);
    }
}
