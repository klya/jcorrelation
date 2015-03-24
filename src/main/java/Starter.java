import com.beust.jcommander.JCommander;
import com.maxifier.jobs.*;
import com.maxifier.sync.SyncJob;
import com.maxifier.sync.impl.ResourceContextImpl;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Time spent:
 * Simple sync framework (interfaces, stabs) 2h
 * Libs (HTTP, DB, Hibrnate, JCommander) 2h
 * Command interface + linking 2h
 * ResourceContextImpl 2h
 * LoadFilesSyncJob 2h
 * StatsSyncJob + ResourceContextImpl 2h
 * ProcessFilesSyncJob 1.5h
 */
public class Starter {
    public static void main(String[] args) throws Exception {
        // Init Http
        Server server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(8080);
        server.addConnector(connector);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{ "index.html" });
        resource_handler.setEtags(true);
        resource_handler.setResourceBase("Data");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
        server.setHandler(handlers);

        server.start();

        // Init hibernate
        HashMap<String, String> override = new HashMap<String, String>();
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("model", override);

        // Init Jobs
        Map<String, SyncJob> jobs = new HashMap<String, SyncJob>();
        jobs.put("Campaign", new CampaignSyncJob());
        jobs.put("LoadFiles", new LoadFilesSyncJob());
        jobs.put("ProcessFiles", new ProcessFilesSyncJob());
        jobs.put("Stats", new StatsSyncJob());
        jobs.put("Show", new ShowSyncJob());

        // Handle job commands
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String line = in.readLine();
            if (line == null || line.isEmpty()) {
                break;
            }

            JCommander commander = new JCommander();
            commander.setProgramName("jcorr");
            for (String command : jobs.keySet()) {
                SyncJob syncJob = jobs.get(command);
                Class<?> configType = (Class<?>) ((ParameterizedType) syncJob.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
                Object config = configType.newInstance();
                commander.addCommand(command, config);
            }

            if (line.equals("help") || line.equals("?")) {
                commander.usage();
                continue;
            }

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                entityManager.getTransaction().begin();
                commander.parse(line.split("\\s+"));
                @SuppressWarnings("unchecked") SyncJob<Object> syncJob = jobs.get(commander.getParsedCommand());
                List<Object> objects = commander.getCommands().get(commander.getParsedCommand()).getObjects();
                syncJob.doJob(new ResourceContextImpl(entityManager), objects.get(0));
            } catch (Exception e) {
                e.printStackTrace(System.out);
                entityManager.getTransaction().setRollbackOnly();
            } finally {
                if (entityManager.getTransaction().getRollbackOnly()) {
                    entityManager.getTransaction().rollback();
                } else {
                    entityManager.getTransaction().commit();
                }
                entityManager.close();
            }
        }

        entityManagerFactory.close();
        server.stop();
    }
}