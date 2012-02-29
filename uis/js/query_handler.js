Ext.ns('org.systemsbiology.hukilau.components');

org.systemsbiology.hukilau.components.QueryHandler = Ext.extend(Object, {
    tab_container: undefined,
    node_query_counter: 0,
    filter_query_counter: 0,

    constructor: function(config) {
        this.tab_container = config.tab_container;

        org.systemsbiology.hukilau.apis.events.MessageBus.on('node_query_submitted', this.doNodeQuery, this);
        org.systemsbiology.hukilau.apis.events.MessageBus.on('filter_query_submitted', this.doFilterQuery, this);
    },

    doNodeQuery: function(uri) {
        this.node_query_counter += 1;
                
        new org.systemsbiology.hukilau.components.QueryResultDisplay({
            parent_container: this.tab_container,
            container_title: 'Node Query ' + this.node_query_counter,
            request: {
                method: 'get',
                uri: uri
            }
        });
    },

    doFilterQuery: function(params) {
        this.filter_query_counter += 1;

        new org.systemsbiology.hukilau.components.QueryResultDisplay({
            parent_container: this.tab_container,
            container_title: 'Filter Query ' + this.filter_query_counter,
            request: {
                method: 'post',
                uri: params.uri,
                params: {
                    filter_list: params.filters
                }
            }
        });
    }
});
