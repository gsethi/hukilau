Ext.onReady(function() {
	var graphQueryTabs = new Ext.TabPanel({
		activeTab: 0,
		items: [
			org.systemsbiology.hukilau.apis.panels.NodeQuery
		]
	});

	var graphControlPanel = {
		xtype: 'panel',
		id: 'graph_control_panel',
		region: 'west',
		contentEl: 'c_graph_control',
		width: 300,
		layout: 'auto',
		layoutConfig: {
			align: 'top'
		},
		items: [
			org.systemsbiology.hukilau.apis.panels.GraphDatabaseSelect,
			graphQueryTabs,
			org.systemsbiology.hukilau.apis.panels.QueryResultInfo,
			org.systemsbiology.hukilau.apis.panels.NodeSchemaSettings,
			org.systemsbiology.hukilau.apis.panels.EdgeSchemaSettings
		]
	};

	var graphDisplayPanel = {
		title: "Graph",
		contentEl: 'c_vis'
	};

	var dataDisplayPanel = new Ext.TabPanel({
		region: 'center',
		id: 'data_display_panel',
		activeTab: 0,
		items: [
			graphDisplayPanel
		]
	});

	new org.systemsbiology.hukilau.components.QueryHandler({
		tab_container: dataDisplayPanel
	});

	new org.systemsbiology.addama.js.TopBar({contentEl: "c_addama_topbar"});

	new Ext.Viewport({
		layout: 'border',
		renderTo: Ext.getBody(),
		items: [
			new Ext.Panel({ contentEl: 'c_addama_topbar', region: 'north', height: 27 }),
			graphControlPanel,
			dataDisplayPanel
		]
	});
});
