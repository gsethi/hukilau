package org.systemsbiology.cancerregulome.hukilau.configs;

import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;

import java.io.*;
import java.util.Map;

/**
 * @author hrovira
 */
public class NetworkMetadataMappingsHandler extends MappingPropertyByIdContainer<JSONObject> implements MappingsHandler {
    public NetworkMetadataMappingsHandler(Map<String, JSONObject> map) {
        super(map);
    }

    public void handle(Mapping mapping) throws Exception {
        String location = mapping.JSON().getString("location");
        File f = new File(location + "/network_metadata.json");
        if (!f.exists()) {
            throw new IllegalArgumentException(location + "/network_metadata.json is required");
        }

        InputStream inputStream = new FileInputStream(f);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line = "";
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                builder.append(line);
            }
        }
        this.addValue(mapping, new JSONObject(builder.toString()));
    }
}
