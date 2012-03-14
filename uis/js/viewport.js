Ext.onReady(function() {
    var workspaceContainer = new org.systemsbiology.hukilau.components.WorkspaceContainer({
        region: "center",
        graphStylesUri: "/addama/stores/graphStyles"
    });

    var graphQueryTabs = new Ext.TabPanel();

    var graphDBSelect = new org.systemsbiology.hukilau.components.queries.GraphDatabaseSelect({
        query_tab_panel: graphQueryTabs,
        workspace_container: workspaceContainer,
        graphUri: "/addama/graphs"
    });

    new Ext.Viewport({
        layout:'border',
        renderTo:Ext.getBody(),
        items:[
            new org.systemsbiology.addama.js.TopBarToolbar({ region:'north', height:30 }),
            {
                xtype:'panel',
                region:'west',
                width:300,
                layout:'auto',
                layoutConfig:{
                    align:'top'
                },
                items:[ graphDBSelect.getPanel(), graphQueryTabs ]
            },
            workspaceContainer
        ]
    });
});
