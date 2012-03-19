package org.systemsbiology.cancerregulome.hukilau.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;

import java.io.*;

/**
 * @author hrovira
 */
public class GraphDBConfiguration {
    public static JSONObject loadConfiguration() throws Exception {
        return loadConfiguration("graphDB.config");
    }

    public static JSONObject loadConfiguration(String configFile) throws Exception {
        FileSystemResource resource = new FileSystemResource(configFile);
        return jsonFromInputStream(resource.getInputStream());
    }

    public static JSONObject loadFromPath(String path) throws Exception {
        return jsonFromInputStream(new FileInputStream(path));
    }

    private static JSONObject jsonFromInputStream(InputStream inputStream) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line = "";
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                builder.append(line);
            }
        }

        return new JSONObject(builder.toString());
    }
}
