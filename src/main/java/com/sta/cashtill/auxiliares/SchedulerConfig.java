
package com.sta.cashtill.auxiliares;

import javax.servlet.*;
import javax.servlet.annotation.*;

import org.quartz.*;
import org.quartz.impl.*;

import com.sta.cashtill.modelo.*;

@WebListener
public class SchedulerConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Crear el Job directamente con la clase CajaHistorica
            JobDetail job = JobBuilder.newJob(CajaHistorica.class)
                    .withIdentity("cajaHistoricaJob", "group1").build();

            // Crear el Trigger para ejecutar cada 10 minutos
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("cajaHistoricaTrigger", "group1")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 */2 * * * ?"))
                    .build();

            // Configurar el Scheduler
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            scheduler.scheduleJob(job, trigger);

            System.out.println("Scheduler configurado correctamente. Job programado cada 2 minutos.");

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                System.out.println("Scheduler detenido correctamente.");
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}