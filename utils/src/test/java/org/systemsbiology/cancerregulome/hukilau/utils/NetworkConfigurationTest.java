package org.systemsbiology.cancerregulome.hukilau.utils;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;
import static org.systemsbiology.cancerregulome.hukilau.utils.GraphDBConfiguration.loadConfiguration;
import static org.systemsbiology.cancerregulome.hukilau.utils.GraphDBConfiguration.loadFromPath;

/**
 * @author hrovira
 */
public class NetworkConfigurationTest {
    public static final String DATABASE_PATH = "target/networkConfigurationTest/test.db";

    @Before
    public void setup() {
        new File(DATABASE_PATH).mkdirs();
    }

    @Test
    public void good() throws Exception {
        NetworkConfiguration nc = new NetworkConfiguration(loadConfiguration("target/test-classes/networkConfigurationTest.json"));
        for (int i = 0; i < 100; i++) nc.incrementNodes();
        for (int i = 0; i < 1000; i++) nc.incrementEdges();

        String dbRootPath = nc.getDatabaseRootPath();
        assertNotNull(dbRootPath);
        assertEquals(DATABASE_PATH, dbRootPath);

        assertEquals(2, nc.getNodeFiles().length());
        assertEquals(7, nc.getEdgeFiles().length());

        nc.outputNetworkMetadata();

        JSONObject actual = loadFromPath("target/networkConfigurationTest/test.db/network_metadata.json");
        assertNotNull(actual);
        assertTrue(actual.has("numberOfNodes"));
        assertEquals(100, actual.getInt("numberOfNodes"));
        assertTrue(actual.has("numberOfEdges"));
        assertEquals(1000, actual.getInt("numberOfEdges"));
        assertTrue(actual.has("nodeTypes"));
        assertEquals(3, actual.getJSONArray("nodeTypes").length());
        assertTrue(actual.has("edgeTypes"));
        assertEquals(7, actual.getJSONArray("edgeTypes").length());
    }
}
