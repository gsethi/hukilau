Ext.onReady(function() {

    new org.systemsbiology.addama.js.TopBar({contentEl: "c_addama_topbar"});
    var ajaxMonitor = new org.systemsbiology.addama.js.widgets.AjaxMonitor();

    var graphQueryTabs = new Ext.TabPanel({
        activeTab: 0,
        items: [
            org.systemsbiology.hukilau.apis.panels.NodeQuery,
            ajaxMonitor.gridPanel
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
        ]
    };

    var graphDisplayPanel = {
        contentEl: 'c_vis',
        region: 'center'
    };

	new Ext.Viewport({
		layout: 'border',
		renderTo: Ext.getBody(),
		items: [
            new Ext.Panel({ contentEl: 'c_addama_topbar', region: 'north', height: 27 }),
			graphControlPanel,
			graphDisplayPanel
		]
	});

});
