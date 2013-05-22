package org.systemsbiology.cancerregulome.hukilau.utils;

import org.neo4j.graphdb.GraphDatabaseService;

import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class DatabaseShutdownHookThread extends Thread {
    private static final Logger log = Logger.getLogger(DatabaseShutdownHookThread.class.getName());

    private final GraphDatabaseService database;

    public DatabaseShutdownHookThread(GraphDatabaseService graphDb) {
        this.database = graphDb;

    }

    public void run() {
        log.info("database shutdown: started");
        try {
            database.shutdown();
        } catch (Exception e) {
            log.warning("database shutdown: " + e.getMessage());
        } finally {
            log.info("database shutdown: complete");
        }
    }
}
