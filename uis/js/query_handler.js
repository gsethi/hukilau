Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.QueryHandler = Ext.extend(Object, {
    tab_container: undefined,
    query_counter: 0,

    constructor: function(config) {
        this.tab_container = config.tab_container;

        org.systemsbiology.hukilau.apis.events.MessageBus.on('node_query_submitted', this.doNodeQuery, this);
    },

    doNodeQuery: function(uri) {
        var that = this;
        this.query_counter += 1;
                
        new org.systemsbiology.hukilau.components.QueryResultDisplay({
            parent_container: that.tab_container,
            container_title: 'Node Query ' + this.query_counter,
            uri: uri
        });
    },
});
