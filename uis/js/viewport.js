Ext.onReady(function() {
    var graphDisplay = new org.systemsbiology.hukilau.components.GraphDisplay({
        cytoscape_content_el: 'c_vis'
    });

    var ajaxMonitor = new org.systemsbiology.addama.js.widgets.AjaxMonitor();

    var dataDisplayPanel = new Ext.TabPanel({
        region: 'center',
        id: 'data_display_panel',
        activeTab: 0,
        items: [
            graphDisplay.getPanel(),
            ajaxMonitor.gridPanel
        ]
    });

    graphDisplay.initCytoscape();

	var graphQueryTabs = new Ext.TabPanel({

	});

	var graphDBSelect = new org.systemsbiology.hukilau.components.queries.GraphDatabaseSelect({
		query_tab_panel: graphQueryTabs,
        data_tab_panel: dataDisplayPanel
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
