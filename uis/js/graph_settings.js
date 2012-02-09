Ext.ns('org.systemsbiology.hukilau.apis.panels');

org.systemsbiology.hukilau.apis.panels.GraphDatabaseSelect = new Ext.Panel({
    id: 'graph_settings_panel',
    title: 'Graph Database',
    autoHeight: true,
    width: 300,
    padding: 5,
    layout: 'form',

    items: [
		{
			xtype: 'combo',
			fieldLabel: 'Graph',
			id: 'graph_database_combo',
			mode: 'remote',
			emptyText: 'Select database',
			autoWidth: true,
			editable: false,
			displayField: 'label',
			valueField: 'uri',
			forceSelection: true,
			triggerAction: 'all',
			store: new Ext.data.JsonStore({
				storeId: 'available_graph_store',
				autoLoad: true,
				proxy: new Ext.data.HttpProxy({
					url: '/addama/graphs',
					method: 'GET'
				}),

				root: 'items',
				fields: ['label', 'uri']
            }),
            listeners: {
        		select: {
        			fn: function(combo, value) {
        				org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_db_selected', {
        					label: value.data.label,
        					uri: value.data.uri
        				});
        				
						var graph_name = value.data.uri.split('graphs/')[1];
        				var meta_uri = '/addama/graphs/' + graph_name + '/metadata';

        				Ext.Ajax.request({
		                    method: "get",
		                    url: meta_uri,
		                    success: function(o) {
		                    	var json = Ext.util.JSON.decode(o.responseText);
								Ext.getCmp('graph_num_of_nodes').setText(json.numberOfNodes);
								Ext.getCmp('graph_num_of_edges').setText(json.numberOfEdges);
								org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_metadata_loaded', json);
		                    }
        				});
        			}
        		}
        	}
        },
		{
			xtype: 'label',
			id: 'graph_num_of_nodes',
			name: 'numberOfNodes',
			fieldLabel: 'Nodes',
			text: '-'
		},
		{
			xtype: 'label',
			id: 'graph_num_of_edges',
			name: 'numberOfEdges',
			fieldLabel: 'Edges',
			text: '-'
		}
    ]
});

org.systemsbiology.hukilau.apis.panels.NodeQuery = new Ext.Panel({
	id: 'graph_query_panel',
	title: 'Node Query',
	layout: 'form',
	header: false,
	autoHeight: true,
	padding: 5,
	items: [
		{
			xtype: 'combo',
			id: 'query_node_type_cmb',
			fieldLabel: 'Node Type',
			emptyText: 'Select node type...',
			displayField: 'name',
			triggerAction: 'all',
			mode: 'local',
			disabled: true,
			forceSelection: true,
			store: new Ext.data.JsonStore({
				autoload: false,
				fields: ['name']
			}),
			listeners: {
				select: function(combo, value) {
					property_cmb = Ext.getCmp('query_node_property_cmb');
					property_cmb.enable();
					property_cmb.store.loadData(value.json.items);					
				}
			}
		},
		{
			xtype: 'combo',
			id: 'query_node_property_cmb',
			fieldLabel: 'Node Property',
			emptyText: 'Select property...',
			displayField: 'name',
			valueField: 'name',
			triggerAction: 'all',
			mode: 'local',
			disabled: true,
			forceSelection: true,
			width: 150,
			store: new Ext.data.JsonStore({
				autoload: false,
				fields: ['name']
			})
		},
		{
			xtype: 'combo',
			id: 'query_comparison_operator',
			fieldLabel: 'Operator',
			mode: 'local',
			width: 100,
			triggerAction: 'all',
			displayField: 'name',
			valueField: 'value',
			value: 'eq',
			store: new Ext.data.JsonStore({
				fields: ['name', 'value'],
				data: [
					{name: 'equals', value: 'eq'}
				]
			})
		},
		{
			xtype: 'textfield',
			id: 'query_term_tf',
			fieldLabel: 'Query Term',
			minLength: 1,
			emptyText: 'Enter query term...'
		},
		{
			xtype: 'combo',
			id: 'query_traversal_cmb',
			fieldLabel: 'Traversal',
			mode: 'local',
			width: 80,
			triggerAction: 'all',
			displayField: 'label',
			valueField: 'value',
			value: '2',
			store: new Ext.data.JsonStore({
				fields: ['label', 'value'],
				data: [ {label: '2 nodes', value: 2},
						{label: '3 nodes', value: 3},
						{label: '4 nodes', value: 4},
						{label: '5 nodes', value: 5} ]
			})
		}
	],
	buttons: [
		{
			id: 'query_submit_btn',
			disabled: true,
			text: 'Query',
			listeners: {
				click: function() {
					var graph_uri = Ext.getCmp('graph_database_combo').getValue();
					var node_type = Ext.getCmp('query_node_type_cmb').getValue();
					var node_prop = Ext.getCmp('query_node_property_cmb').getValue();
					var query_term = Ext.getCmp('query_term_tf').getValue();
					var level = Ext.getCmp('query_traversal_cmb').getValue();

					if (node_prop == '') {
						Ext.MessageBox.alert('Error', 'Please select a node property.', function() {
							Ext.getCmp('query_node_property_cmb').focus();
						});

						return;
					}

					if (query_term == '') {
						Ext.MessageBox.alert('Error', 'Please enter a query term.', function() {
							Ext.getCmp('query_term_tf').focus();
						});

						return;
					}

					var query_uri = graph_uri + '/query?query={' + node_prop + '=\"' + query_term + '\"}' +
									'&level=' + level +
									'&nodeLabel=' + node_prop;
					
					org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('node_query_submitted', query_uri);
				}
			}
		}
	],
	listeners: {
		render: function() {
			var graph_meta_handler = function(d) {
				node_type_cmb = Ext.getCmp('query_node_type_cmb');
				node_type_cmb.enable();
				node_type_cmb.store.loadData(d.nodeTypes);

				var btn = Ext.getCmp('query_submit_btn');
				btn.enable();
			}
			
			org.systemsbiology.hukilau.apis.events.MessageBus.on('graph_metadata_loaded', graph_meta_handler, this);
		}
	}
});

org.systemsbiology.hukilau.apis.panels.QueryResultInfo = new Ext.Panel({
	title: 'Query Result',
	layout: 'form',
	autoHeight: true,
	padding: 5,
	items: [
		{
			xtype: 'label',
			id: 'num_of_nodes_in_query',
			name: 'numberOfNodesInQuery',
			fieldLabel: 'Nodes',
			text: '-'
		},
		{
			xtype: 'label',
			id: 'num_of_edges_in_query',
			name: 'numberOfEdgesInQuery',
			fieldLabel: 'Edges',
			text: '-'
		}		
	],
	listeners: {
		render: function() {
			var query_result_handler = function(d) {
				Ext.getCmp('num_of_nodes_in_query').setText(d.data.numberOfNodes);
				Ext.getCmp('num_of_edges_in_query').setText(d.data.numberOfEdges);
			}
			
			org.systemsbiology.hukilau.apis.events.MessageBus.on('query_result_available', query_result_handler, this);
		}
		
	}
});

org.systemsbiology.hukilau.apis.panels.FilterResultInfo = new Ext.Panel({
	id: 'filter_result_panel',
	title: 'Filter Result',
	layout: 'form',
	autoHeight: true,
	padding: 5,
	items: [
		{
			xtype: 'label',
			id: 'num_of_filtered_nodes',
			name: 'numberOfFilteredNodes',
			fieldLabel: 'Nodes',
			text: '-'
		},
		{
			xtype: 'label',
			id: 'num_of_filtered_edges',
			name: 'numberOfFilteredEdges',
			fieldLabel: 'Edges',
			text: '-'
		}		
	],
	listeners: {
		render: function() {
			var filtered_graph_handler = function(d) {
				Ext.getCmp('num_of_filtered_nodes').setText(d.data.numberOfNodes);
				Ext.getCmp('num_of_filtered_edges').setText(d.data.numberOfEdges);
			}
			
			org.systemsbiology.hukilau.apis.events.MessageBus.on('filtered_graph_available', filtered_graph_handler, this);
		}
		
	}
});

