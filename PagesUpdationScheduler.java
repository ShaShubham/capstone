package com.akqa.core.schedulers.capstoneScheduler;

import com.akqa.core.servlets.PagesAddHelp;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;


@Component(immediate = true, service = Runnable.class)
@Designate(ocd = PagesUpdationSchedulerConfiguration.class)
public class PagesUpdationScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PagesUpdationScheduler.class);
    private int schedulerId;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    protected void activate(PagesUpdationSchedulerConfiguration config) {
        schedulerId = config.schedulerName().hashCode();
        addScheduler(config);
    }

    @Deactivate
    protected void deactivate(PagesUpdationSchedulerConfiguration config) {
        removeScheduler();
    }

    protected void removeScheduler() {
        scheduler.unschedule(String.valueOf(schedulerId));
    }

    protected void addScheduler(PagesUpdationSchedulerConfiguration config) {
        ScheduleOptions scheduleOptions = scheduler.EXPR(config.cronExpression());
        scheduleOptions.name(String.valueOf(schedulerId));
        scheduler.schedule(this, scheduler.NOW()); // run scheduler immediiately
        scheduler.schedule(this, scheduleOptions); // runs every one minute
    }
    public static String pagesUpdationTime;
    public static Set<String> removedDataSeoPath;
    private final String serviceName = "capstoneserviceuser";
    @Override //runs every one minute
    public void run() {
        LOG.info("\nentered in run function in scheduler\n");

        final Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(ResourceResolverFactory.SUBSERVICE, serviceName);
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(paramMap);
        } catch (LoginException e) {
            LOG.info("\n\n##### @69 error " + e.getMessage() + "\n");
            throw new RuntimeException(e);
        }
        Resource resource = resourceResolver.getResource("/content/capstone");

        if (removedDataSeoPath!=null) {
            try {
                LOG.info(removedDataSeoPath.toString() + " @76");
                PagesAddHelp ph = new PagesAddHelp(resource);
                ph.updatePages();
            } catch (Exception e) {
                LOG.info(e.getMessage());
            }
        }

        removedDataSeoPath = new HashSet<>();


        // to note that at what time the word was stored
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        pagesUpdationTime = formatter.format(date);


        LOG.info("Last line in run function || Updation time: " + pagesUpdationTime+"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

    }
}