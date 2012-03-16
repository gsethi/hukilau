Ext.onReady(function() {
    var workspaceContainer = new org.systemsbiology.hukilau.components.WorkspaceContainer({
        region: "center",
        tabPosition: "top",
        graphStylesUri: "/graphStyles.json"
    });

    var graphQueryTabs = new Ext.TabPanel();

    var graphDBSelect = new org.systemsbiology.hukilau.components.queries.GraphDatabaseSelect({
        query_tab_panel: graphQueryTabs,
        workspace_container: workspaceContainer,
        graphsUri: "/hukilau-svc/graphs"
    });

    new Ext.Viewport({
        layout:'border',
        renderTo:Ext.getBody(),
        items:[
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
