Ext.ns('org.systemsbiology.hukilau.components');

var QueryHandler = Ext.extend(Object, {
	array_to_uri_map: function(p_array) {
		var uri_map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			uri_map[p_array[i].uri] = p_array[i];
		}

		return uri_map;
	},

    doQuery: function(uri) {
        var that = this;
        Ext.Ajax.request({
            method: "get",
            url: uri,
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                
                if (json.data.numberOfEdges > 0 || json.data.numberOfNodes > 0) {
                	org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_dataschema_available', json.dataSchema);
                	org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('query_result_available', json);
                }
            },
            failure: function(o) {
                console.log('Query failed:');
                console.log(o.responseText);
            }
        });
    },
    
    getCurrentData: function() {
    	return this.current_data;
    },

    getCurrentDataSchema: function() {
    	return this.current_data_schema;
    }
});

org.systemsbiology.hukilau.components.QueryHandler = new QueryHandler();

org.systemsbiology.hukilau.apis.events.MessageBus.on('node_query_submitted',
    org.systemsbiology.hukilau.components.QueryHandler.doQuery,
    org.systemsbiology.hukilau.components.QueryHandler);
