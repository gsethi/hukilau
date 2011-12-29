var topBarPanel = new Ext.Panel({
	contentEl: 'c_addama_topbar',
	region: 'north',
	height: 27
});

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
		graphQueryTabs
	],
	bbar: [
		{
			text: "Refresh UI",
			listeners: {
				click: function() {
	                Ext.Ajax.request({ method: "POST", url: "/addama/ui/refresh",
	                    success: function() {
	                        document.location = document.location.href;
	                    }
	                });
				}
			}
		}
	]
};

var graphDisplayPanel = {
	contentEl: 'c_vis',
	region: 'center'
};

Ext.onReady(function() {
	new Ext.Viewport({
		layout: 'border',
		renderTo: Ext.getBody(),
		items: [
			topBarPanel,
			graphControlPanel,
			graphDisplayPanel
		]
	})

	new TopBar("c_addama_topbar");
});
