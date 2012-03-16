package org.systemsbiology.cancerregulome.hukilau.utils;

import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.server.database.Database;

import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class DatabaseShutdownHookThread extends Thread {
    private static final Logger log = Logger.getLogger(DatabaseShutdownHookThread.class.getName());

    private final AbstractGraphDatabase database;

    public DatabaseShutdownHookThread(AbstractGraphDatabase graphDb) {
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
