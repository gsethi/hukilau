package org.systemsbiology.cancerregulome.hukilau.utils;

import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.EmbeddedServerConfigurator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author hrovira
 */
public class EmbeddedNeo4j implements InitializingBean, DisposableBean {
    private EmbeddedGraphDatabase graphDB;
    private WrappingNeoServerBootstrapper neoServer;

    public void setGraphDB(EmbeddedGraphDatabase graphDB) {
        this.graphDB = graphDB;
    }

    public void afterPropertiesSet() throws Exception {
        //TODO: Could put this in a config file....
        EmbeddedServerConfigurator config = new EmbeddedServerConfigurator(this.graphDB);
        config.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, 7474);
        config.configuration().setProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, "0.0.0.0");

        this.neoServer = new WrappingNeoServerBootstrapper(this.graphDB, config);
        this.neoServer.start();
    }

    public void destroy() throws Exception {
        this.neoServer.stop();
        this.graphDB.shutdown();
    }

}
