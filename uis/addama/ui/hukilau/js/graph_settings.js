Ext.ns('org.systemsbiology.hukilau.apis.panels')

org.systemsbiology.hukilau.apis.panels.GraphDatabaseSelect = new Ext.form.FieldSet({
    id: 'graph_settings_panel',
    name: 'graph_settings_panel',
    title: 'Graph Database',
    autoScroll: false,
    autoHeight: true,
    autoWidth: true,
    layout: 'form',

    items: [
		{
			xtype: 'combo',
			fieldLabel: 'Graph',
			id: 'graph_database_combo',
			mode: 'remote',
			emptyText: 'Select database',
			autoWidth: false,
			width: 300,
			editable: false,
			displayField: 'label',
			valueField: 'uri',
			forceSelection: true,
			triggerAction: 'all',
			store: new Ext.data.JsonStore({
				storeId: 'available_graph_store',
				autoLoad: true,
				proxy: new Ext.data.HttpProxy({
					url: '/addama/datasources/graphs',
					method: 'GET'
				}),

				root: 'items',
				fields: ['label', 'uri'],
            }),
            listeners: {
        		select: {
        			fn: function(combo, value) {
        				org.systemsbiology.hukilau.apis.events.MessageBus.fireEvent('graph_db_selected', {
        					label: value.data.label,
        					uri: value.data.uri
        				});
        				
						var graph_name = value.data.uri.split('graphs/')[1];
						var meta_uri = '/addama/workspaces/fileshare/graphs/' + graph_name + '/metadata.json';
        				
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

Ext.onReady(function() {
	org.systemsbiology.hukilau.apis.panels.GraphDatabaseSelect.render('c_graphdb_select');
});
