Ext.ns('org.systemsbiology.hukilau.components');

var QueryHandler = Ext.extend(Object, {
	current_data: {
		nodes: undefined,
		node_array: undefined,
		edges: undefined,
		edge_array: undefined,
	},

	current_data_schema: {
		nodes: undefined,
		edges: undefined
	},

	array_to_uri_map: function(p_array) {
		var uri_map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			uri_map[p_array[i].uri] = p_array[i];
		}

		return uri_map;
	},

	processData: function(json) {
		this.current_data.nodes = this.array_to_uri_map(json.data.nodes);
		this.current_data.node_array = json.data.nodes;
		this.current_data.edges = this.array_to_uri_map(json.data.edges);
		this.current_data.edge_array = json.data.edges;
		
		this.current_data_schema.nodes = json.dataSchema.nodes;
		this.current_data_schema.edges = json.dataSchema.edges;
	},

    doQuery: function(uri) {
        var that = this;
        Ext.Ajax.request({
            method: "get",
            url: uri,
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                that.processData(json);
                org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_dataschema_available', json.dataSchema);
                org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('query_result_available', json);
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
