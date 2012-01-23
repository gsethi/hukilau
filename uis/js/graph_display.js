Ext.ns('org.systemsbiology.hukilau.components');

var GraphDisplay = Ext.extend(Object, {
    vis: undefined,

    constructor: function(config) {
        Ext.apply(this, config);

        GraphDisplay.superclass.constructor.call(this);
    },

    doQuery: function(uri) {
        Ext.Ajax.request({
            method: "GET",
            url: uri,
            success: function(o) {
                this.redraw(Ext.util.JSON.decode(o.responseText));
            },
            failure: function(o) {
                console.log('Query failed:');
                console.log(o.responseText);
                org.systemsbiology.addama.js.Message.error("Query Failed", o.responseText);
            },
            scope: this
        });
    },

    redraw: function(json) {
        if (this.vis === undefined) {
            this.vis = new org.cytoscapeweb.Visualization(this.contentEl, {
                swfPath: "https://informatics-apps.systemsbiology.net/cytoscapeweb_v0.8/swf/CytoscapeWeb",
                flashInstallerPath: "https://informatics-apps.systemsbiology.net/cytoscapeweb_v0.8/swf/playerProductInstall"
            });            
        }
        
        this.vis.draw({
            network: json,
            nodeTooltipsEnabled: true,
            edgeTooltipsEnabled: true
        });        
    }
});

Ext.onReady(function() {
    org.systemsbiology.hukilau.components.GraphDisplay = new GraphDisplay({contentEl: 'c_vis'});

    org.systemsbiology.hukilau.apis.events.MessageBus.on('node_query_submitted',
        org.systemsbiology.hukilau.components.GraphDisplay.doQuery,
        org.systemsbiology.hukilau.components.GraphDisplay);

});
