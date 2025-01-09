package com.sta.cashstill.auxiliares;

import javax.servlet.*;
import javax.servlet.annotation.*;

import org.quartz.*;
import org.quartz.impl.*;

import com.sta.cashstill.modelo.*;

@WebListener
public class SchedulerConfig implements ServletContextListener {

    private Scheduler scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Crear el Job directamente con la clase CajaHistorica
            JobDetail job = JobBuilder.newJob(CajaHistorica.class)
                    .withIdentity("cajaHistoricaJob", "group1").build();

            // Crear el Trigger para ejecutar cada día a las 23:59
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("cajaHistoricaTrigger", "group1")
                    .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(23, 59))
                    .build(); 
            
         // Crear el Trigger para ejecutar cada 5 minutos
       /*     Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("cajaHistoricaTrigger", "group1")
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(5) // Intervalo de 5 minutos
                            .repeatForever()) // Repetir indefinidamente
                    .build(); */

            // Configurar el Scheduler
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            scheduler.scheduleJob(job, trigger);

            System.out.println("Scheduler configurado correctamente. Job programado 23:59Hs.");

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            try {
                if (!scheduler.isShutdown()) {
                    scheduler.shutdown(true); // Espera a que los trabajos finalicen antes de cerrar
                    System.out.println("Scheduler detenido correctamente.");
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }
}
