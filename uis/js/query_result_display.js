Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.QueryResultDisplay = Ext.extend(Object,{
	node_prop_store: undefined,
	node_grid: undefined,
	edge_prop_store: undefined,
	edge_grid: undefined,
	grid_container: undefined,
	data_schema: undefined,

	parent_container: undefined,
	container_title: undefined,

	constructor: function(config) {
		if (config.parent_container === undefined) {
			console.log("Error: Parent container for query result is not defined.")
			return;
		}

		this.parent_container = config.parent_container;
		this.container_title = config.container_title === undefined ? "Query Result" : config.container_title;

		var that = this;
		var uri = config.uri;
		
        Ext.Ajax.request({
	        method: "get",
	        url: uri,
	        success: function(o) {
	            var json = Ext.util.JSON.decode(o.responseText);

	            if (json.data.numberOfEdges > 0 || json.data.numberOfNodes > 0) {
	            	org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_dataschema_available', json.dataSchema);

	                that.create_grids(json);
	            }
	            else {
	            	that.show_error("Query result is empty");
	            }
	        },
	        failure: function(o) {
	            that.show_Error("Query failed")
	        }
		});
	},

	show_error: function(error_msg) {
		var error_panel = new Ext.Panel({
			title: this.container_title,
			html: '<div class="query_error">' + error_msg + '</div>',
			closable: true,			
		});

		this.parent_container.add(error_panel);
		this.parent_container.activate(error_panel);
	},

	create_grids: function(data) {
		var that = this;

		this.node_prop_store = new Ext.data.JsonStore({
			data: {
				rows: data.data.nodes,
				metaData: {
					fields: data.dataSchema.nodes,
					root: 'rows'
				}
			}
		});

		this.hide_fields(data.dataSchema.nodes, ['name', 'label', 'id', 'uri'], this.find_field_indices(data.dataSchema.nodes));
		var node_columns = this.create_node_columns(data.dataSchema.nodes);
        this.node_grid = new Ext.grid.GridPanel({
		    title: 'Nodes',
		    region: 'center',
		    autoScroll: true,
		    autoWidth: true,
		    loadMask: true,
		    disabled: false,
		    ds: this.node_prop_store,
		    cm: new Ext.grid.ColumnModel(node_columns),
		    tbar: [
		    	{
		    		text: "Add Nodes",
		    		handler: function() {
		    			var gm = org.systemsbiology.hukilau.components.GraphManager;

		    			rows = that.node_grid.getSelectionModel().getSelections();
		    			gm.addNodes(rows);
		    		}
		    	}
		    ]
		});

		this.add_node_label_fields(data.dataSchema.edges);
		this.add_node_labels(data.data.edges, data.data.nodes);
		this.edge_prop_store = new Ext.data.JsonStore({
			data: {
				rows: data.data.edges,
				metaData: {
					fields: data.dataSchema.edges,
					root: 'rows'
				}
			}
		});

		this.hide_fields(data.dataSchema.edges, ['name', 'label', 'id', 'uri', 'source', 'target'], this.find_field_indices(data.dataSchema.edges));
		var edge_columns = this.create_edge_columns(data.dataSchema.edges);
		this.edge_grid = new Ext.grid.GridPanel({
		    title: 'Edges',
		    region: 'south',
		    split: true,
		    height: 300,
		    minSize: 300,
		    autoScroll: true,
		    autoWidth: true,
		    loadMask: true,
		    disabled: false,
		    ds: this.edge_prop_store,
		    cm: new Ext.grid.ColumnModel(edge_columns),
		    tbar: [
		    	{
		    		text: "Add Edges",
		    		handler: function() {
		    			var gm = org.systemsbiology.hukilau.components.GraphManager;
		    			var node_uris = {};    			

		    			var rows = that.edge_grid.getSelectionModel().getSelections();
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

		    			var node_rows = that.node_grid.getStore().queryBy(filter_fn);
		    			gm.addNodes(node_rows.items);
		    			gm.addEdges(rows);
		    		}
		    	}
		    ]
		});

		this.grid_container = new Ext.Panel({
			title: this.container_title,
			layout: 'border',
			closable: true,
			items: [
				this.node_grid,
				this.edge_grid
			]
		});

		this.parent_container.add(this.grid_container);
		this.parent_container.activate(this.grid_container);
    },

    get_node_grid: function() {
    	return this.node_grid;
    },

    get_edge_grid: function() {
    	return this.edge_grid;
    },

	build_column_model: function(meta_data) {
		var columns = [];

        for (var i = 0; i < meta_data.length; i++ ) {
            var field = meta_data[i];

            var col = {
            	xtype: 'gridcolumn',
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
		
		return columns;
	},

	map_field: function(p_array, key) {
		var map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			map[p_array[i][key]] = p_array[i];
		}

		return map;
	},

	find_field_indices: function(p_array) {
		var field_map = {};
		var n = p_array.length;

		for (var i = 0; i < n; i++) {
			field_map[p_array[i].name] = i;
		}

		return field_map;
	},

	hide_fields: function(meta, fields, name_to_index_map) {
		var n = fields.length;
		for (var i = 0; i < n; i++) {
			if (name_to_index_map.hasOwnProperty(fields[i])) {
				var index = name_to_index_map[fields[i]];
				meta[index].hidden = true;
			}
		}
	},

	create_node_columns: function(schema) {		
		return this.build_column_model(schema);
	},

	create_edge_columns: function(schema) {
		var columns = this.build_column_model(schema);
		        
		columns.push({
			xtype: 'gridcolumn',
			name: "source_label",
			header: "source",
			dataIndex: "source_label"
		});

		columns.push({
			xtype: 'gridcolumn',
			name: "target_label",
			header: "target",
			dataIndex: "target_label"
		});

		return columns;
	},

	add_node_label_fields: function(edge_schema) {
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
	},

	add_node_labels: function(edges, nodes) {
		var nodes_by_uri = this.map_field(nodes, 'uri');
		var n = edges.length;
		for (var i = 0; i < n; i++) {
			var edge = edges[i];
			edge.source_label = nodes_by_uri[edge.source].label;
			edge.target_label = nodes_by_uri[edge.target].label;
		}
	}
});
