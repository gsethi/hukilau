package org.systemsbiology.cancerregulome.hukilau.utils;

import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author hrovira
 */
public class GraphDBConfiguration {
    public static JSONObject loadConfiguration() throws Exception {
        return loadConfiguration("graphDB.config");
    }

    public static JSONObject loadConfiguration(String configFile) throws Exception {
        ClassPathResource resource = new ClassPathResource(configFile);
        InputStream inputStream = resource.getInputStream();
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
