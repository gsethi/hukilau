Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.WorkspaceContainer = Ext.extend(Ext.TabPanel,{
    graphStylesUri: "/addama/stores/graphStyles",
    graph_styles: {},
    tabPosition: "bottom",

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.hukilau.components.WorkspaceContainer.superclass.constructor.apply(this, arguments);

        this.loadStyleObjects();
    },

    initComponent: function() {
        this.workspace_setup = new org.systemsbiology.hukilau.components.WorkspaceSetup({
            title: 'Workspace Setup',
            workspace_container: this
        });

        Ext.apply(this, {
            region: 'center',
            activeItem: 0,
            items: [
                this.workspace_setup
            ]
        });

        org.systemsbiology.hukilau.components.WorkspaceContainer.superclass.initComponent.call(this);
    },

    createWorkspace: function(config) {
        Ext.apply(config, {
            default_visual_style: this.getGraphStyle(config.data.id)
        });

        var workspace = new org.systemsbiology.hukilau.components.Workspace(config);

        this.add(workspace);
        this.setActiveTab(workspace);

        return workspace;
    },

    getWorkspace: function(graph_id) {
        if (this.workspaces.hasOwnProperty(graph_id)) {
            return this.workspaces[graph_id];
        }
    },

    loadStyleObjects: function() {
        Ext.Ajax.request({
            method: "GET",
            url: this.graphStylesUri,
            scope: this,
            success: function(o) {
                var data = Ext.util.JSON.decode(o.responseText);
                Ext.each(data.items, function(item) {
                    this.graph_styles[item.id] = item.style;
                }, this);
            }
        });
    },

    getGraphStyle: function(graph_id) {
        if (this.graph_styles.hasOwnProperty(graph_id)) {
            return this.graph_styles[graph_id];
        }
        else {
            return this.graph_styles["default"];
        }
    }
});
