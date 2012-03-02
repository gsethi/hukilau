Ext.onReady(function() {
	var graphQueryTabs = new Ext.TabPanel({

	});

	var graphDBSelect = new org.systemsbiology.hukilau.components.queries.GraphDatabaseSelect({
		query_tab_panel: graphQueryTabs
	});

	var graphControlPanel = {
		xtype: 'panel',
		id: 'graph_control_panel',
		region: 'west',
		width: 300,
		layout: 'auto',
		layoutConfig: {
			align: 'top'
		},
		items: [
			graphDBSelect.getPanel(),
			graphQueryTabs
		]
	};

	var graphDisplayPanel = {
		title: "Graph",
		contentEl: 'c_vis'
	};

    var ajaxMonitor = new org.systemsbiology.addama.js.widgets.AjaxMonitor();

	var dataDisplayPanel = new Ext.TabPanel({
		region: 'center',	
		id: 'data_display_panel',
		activeTab: 0,
		items: [
			graphDisplayPanel,  ajaxMonitor.gridPanel
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
