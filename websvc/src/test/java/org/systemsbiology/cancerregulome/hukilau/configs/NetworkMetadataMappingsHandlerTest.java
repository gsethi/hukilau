package org.systemsbiology.cancerregulome.hukilau.configs;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.systemsbiology.addama.jsonconfig.Mapping;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author hrovira
 */
public class NetworkMetadataMappingsHandlerTest {
    private final Map<String, JSONObject> networkMetadataById = new HashMap<String, JSONObject>();
    private NetworkMetadataMappingsHandler handler;

    @Before
    public void setup() {
        handler = new NetworkMetadataMappingsHandler(networkMetadataById);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wo_metadata_test() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "wo_metadata_test");
        json.put("label", "wo_metadata_test");
        json.put("location", "target/test-classes/wo_metadata_test.db");
        handler.handle(new Mapping(json));
    }

    @Test
    public void wi_metadata_test() throws Exception {
        JSONObject json = new JSONObject();
        json.put("id", "wi_metadata_test");
        json.put("label", "wi_metadata_test");
        json.put("location", "target/test-classes/wi_metadata_test.db");
        handler.handle(new Mapping(json));

        assertTrue(networkMetadataById.containsKey("wi_metadata_test"));
        JSONObject actual = networkMetadataById.get("wi_metadata_test");
        assertNotNull(actual);
        assertTrue(actual.has("numberOfNodes"));
        assertTrue(actual.has("numberOfEdges"));
        assertTrue(actual.has("nodeTypes"));
        assertTrue(actual.has("edgeTypes"));

        assertEquals(actual.getInt("numberOfNodes"), 100);
        assertEquals(actual.getInt("numberOfEdges"), 1000);
        assertEquals(actual.getJSONArray("nodeTypes").length(), 1);
        assertEquals(actual.getJSONArray("edgeTypes").length(), 1);
    }
}
