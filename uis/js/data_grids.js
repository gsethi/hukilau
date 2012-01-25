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

    			rows = Ext.getCmp('node_prop_grid').getSelectionModel().getSelections();
    			gm.addNodes(rows);
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
    			//var qh = org.systemsbiology.hukilau.components.QueryHandler;
    			var gm = org.systemsbiology.hukilau.components.GraphManager;
    			var node_uris = {};    			

    			rows = Ext.getCmp('edge_prop_grid').getSelectionModel().getSelections();
    			var n = rows.length;
    			for (var i = 0; i < n; i++) {
    				var edge_data = rows[i].data;
    				node_uris[edge_data.source] = true;
    				node_uris[edge_data.target] = true;
    			}

    			// Build a filter function for finding the source and target node rows for each edge
    			var filter_fn = function(record, id) {
    				if (node_uris.hasOwnProperty(record.data.uri)) {
    					return true;
    				}
    				else {
    					return false;
    				}
    			};

    			var node_grid = Ext.getCmp('node_prop_grid');
    			var node_rows = node_grid.getStore().queryBy(filter_fn);
    			gm.addNodes(node_rows.items);
    			gm.addEdges(rows);
    		}
    	}
    ]
});

org.systemsbiology.hukilau.apis.panels.showGridPanels = function(query_result) {
	var current_data = query_result.data;

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
	};

	var map_field = function(p_array, key) {
		var map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			map[p_array[i][key]] = p_array[i];
		}

		return map;
	};

	var find_field_indices = function(p_array) {
		var field_map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			field_map[p_array[i].name] = i;
		}

		return field_map;
	};

	var hide_fields = function(meta, fields, name_to_index_map) {
		var n = fields.length;
		for (var i = 0; i < n; i++) {
			if (name_to_index_map.hasOwnProperty(fields[i])) {
				var index = name_to_index_map[fields[i]];
				meta[index].hidden = true;
			}
		}
	};

	var node_schema = query_result.dataSchema.nodes;
	hide_fields(node_schema, ['name', 'label', 'id', 'uri'], find_field_indices(node_schema));
	var node_data = {
		rows: current_data.nodes,
		metaData: {
			fields: node_schema,
			root: 'rows'
		}
	};

	var node_prop_store = this.createPropStore(this.nodePropGridPanel.getId());
	this.nodePropGridPanel.enable();
	this.nodePropGridPanel.store.loadData(node_data);

	var edge_schema = query_result.dataSchema.edges;
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
	
	var nodes_by_uri = map_field(current_data.nodes, 'uri');
	var n = current_data.edges.length;
	for (var i = 0; i < n; i++) {
		var edge = current_data.edges[i];
		edge.source_label = nodes_by_uri[edge.source].label;
		edge.target_label = nodes_by_uri[edge.target].label;
	}
	
	var edge_data = {
		rows: current_data.edges,
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
