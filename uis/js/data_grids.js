Ext.ns('org.systemsbiology.hukilau.apis.panels')

org.systemsbiology.hukilau.apis.panels.createPropStore = function(grid_id) {
    var ds = new Ext.data.Store({
        reader: new Ext.data.JsonReader({
	        root: 'rows'
	    }),
	    listeners: {
	    	metachange: {
	    		fn: function(store, meta) {
	    			var columns = [];

	                for (var i = 0; i < meta.fields.length; i++ ) {
	                    var field = meta.fields[i];

	                    var col = {
		                    header: field.name,
		                    type: field.type,
		                    dataIndex: field.name
		                };

		                if (field.header) {
		                	col.header = field.header;
		                }
		                else {
		                	col.header = field.name;
		                }

		                if (field.hidden) {
		                	col.hidden = true;
		                	col.sortable = false;
		                }
		                else {
		                	col.sortable = true;
		                }

	                    columns.push(col);
	                }

	                var grid = Ext.getCmp(grid_id);
	                grid.reconfigure(store, new Ext.grid.ColumnModel({
		                columns: columns,
		                sortable: true
		            }));
	    		}
	    	}
	    }
    });

    return ds;
};

org.systemsbiology.hukilau.apis.panels.nodePropContextMenu = new Ext.menu.Menu({
    items: [
        {
            text: 'Add nodes'
        },
        {
            text: 'Add nodes with first neighbors'
        }
    ],
    listeners: {
    	itemclick: function(item) {
    		console.log("nodepropclick");
    		console.log(items);
		}
	}
});

org.systemsbiology.hukilau.apis.panels.nodePropGridPanel = new Ext.grid.GridPanel({
	id: 'node_prop_grid',
    name: 'node_prop_grid',
    title: 'Nodes',
    region: 'center',
    autoScroll: true,
    autoWidth: true,
    loadMask: true,
    disabled: false,
    ds: org.systemsbiology.hukilau.apis.panels.createPropStore('node_prop_grid'),
    cm: new Ext.grid.ColumnModel([]),
    tbar: [
    	{
    		text: "Add Nodes",
    		handler: function() {
    			var gm = org.systemsbiology.hukilau.components.GraphManager;
    			var uris = [];

    			rows = Ext.getCmp('node_prop_grid').getSelectionModel().getSelections();
    			var n = rows.length;
    			for (var i = 0; i < n; i++) {
    				uris.push(rows[i].data.uri);
    			}

    			gm.addNodes(uris);
    			//org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('add_nodes_to_graph', rows);
    		}
    	}
    ]
});

org.systemsbiology.hukilau.apis.panels.edgePropGridPanel = new Ext.grid.GridPanel({
    id: 'edge_prop_grid',
    name: 'edge_prop_grid',
    title: 'Edges',
    region: 'south',
    split: true,
    height: 300,
    minSize: 300,
    autoScroll: true,
    autoWidth: true,
    loadMask: true,
    disabled: false,
    ds: org.systemsbiology.hukilau.apis.panels.createPropStore('edge_prop_grid'),
    cm: new Ext.grid.ColumnModel([]),
    tbar: [
    	{
    		text: "Add Edges",
    		handler: function() {
    			var qh = org.systemsbiology.hukilau.components.QueryHandler;
    			var gm = org.systemsbiology.hukilau.components.GraphManager;
    			var uris = [];

    			rows = Ext.getCmp('edge_prop_grid').getSelectionModel().getSelections();
    			var n = rows.length;
    			for (var i = 0; i < n; i++) {
    				uris.push(rows[i].data.uri);
    			}

    			gm.addEdges(uris);

    			//org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('add_edges_to_graph', rows);
    		}
    	}
    ]
});

org.systemsbiology.hukilau.apis.panels.showGridPanels = function() {
	var qh = org.systemsbiology.hukilau.components.QueryHandler;
	var current_data = qh.getCurrentData();

	var copy_metadata = function(p_array) {
		var meta = [];
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			meta.push( {
				name: p_array[i].name,
				type: p_array[i].type
			});
		}

		return meta;
	}

	var find_field_indices = function(p_array) {
		var field_map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			field_map[p_array[i].name] = i;
		}

		return field_map;
	}

	// var get_uri_to_label_map = function(p_array) {
	// 	var label_map = {};
	// 	var n = p_array.length;

	// 	for (var i = 0; i < n; i++) {
	// 		label_map[p_array[i].uri] = p_array[i].label;
	// 	}

	// 	return label_map;
	// }

	var hide_fields = function(meta, fields, name_to_index_map) {
		var n = fields.length;
		for (var i = 0; i < n; i++) {
			if (name_to_index_map.hasOwnProperty(fields[i])) {
				var index = name_to_index_map[fields[i]];
				meta[index].hidden = true;
			}
		}
	};

	// var node_schema = copy_metadata(d.dataSchema.nodes);
	var node_schema = copy_metadata(qh.getCurrentDataSchema().nodes);
	hide_fields(node_schema, ['name', 'label', 'id', 'uri'], find_field_indices(node_schema));
	var node_data = {
		rows: current_data.node_array,
		metaData: {
			fields: node_schema,
			root: 'rows'
		}
	};

	var node_prop_store = this.createPropStore(this.nodePropGridPanel.getId());
	this.nodePropGridPanel.enable();
	this.nodePropGridPanel.store.loadData(node_data);

	//var edge_schema = copy_metadata(d.dataSchema.edges);
	var edge_schema = copy_metadata(qh.getCurrentDataSchema().edges);
	edge_fields = find_field_indices(edge_schema);
	hide_fields(edge_schema, ['name', 'label', 'id', 'uri', 'source', 'target'], edge_fields);

	edge_schema.push({
		name: "source_label",
		header: "source",
		dataIndex: "source_label"
	});

	edge_schema.push({
		name: "target_label",
		header: "target",
		dataIndex: "target_label"
	});
	
	var n = current_data.edge_array.length;
	for (var i = 0; i < n; i++) {
		var edge = current_data.edge_array[i];
		edge.source_label = current_data.nodes[edge.source].label;
		edge.target_label = current_data.nodes[edge.target].label;
	}
	
	var edge_data = {
		rows: current_data.edge_array,
		metaData: {
			fields: edge_schema,
			root: 'rows'
		}
	};

	var edge_prop_store = this.createPropStore(this.edgePropGridPanel.getId());
	this.edgePropGridPanel.enable();
	this.edgePropGridPanel.store.loadData(edge_data);
};

org.systemsbiology.hukilau.apis.events.MessageBus.on('query_result_available',
													  org.systemsbiology.hukilau.apis.panels.showGridPanels,
													  org.systemsbiology.hukilau.apis.panels);
