package tlb;

import org.apache.cassandra.service.EmbeddedCassandraService;
import tlb.server.ServerInitializer;
import tlb.server.TlbServerInitializer;
import tlb.utils.SystemEnvironment;

/**
 * @understands launching tlb server
 */
public class ServerApp {
    public static void main(String[] args) {
        try {
            System.setProperty("log4j.configuration", "log4j.properties");
            EmbeddedCassandraService cassandra = new EmbeddedCassandraService();
            cassandra.start();
            new TlbServerInitializer(new SystemEnvironment()).init().start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
