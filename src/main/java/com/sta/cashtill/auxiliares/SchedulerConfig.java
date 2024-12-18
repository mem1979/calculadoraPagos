package com.sta.cashtill.auxiliares;

import org.quartz.*;
import org.quartz.impl.*;

import com.sta.cashtill.modelo.*;

public class SchedulerConfig {
    public static void main(String[] args) {
        try {
            // Crear el Job directamente con la clase CajaHistorica
            JobDetail job = JobBuilder.newJob(CajaHistorica.class)
                    .withIdentity("cajaHistoricaJob", "group1").build();

            // Crear el Trigger para las 23:59 de lunes a sábado
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("dailyTrigger", "group1")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?"))
                    .build();

            // Configurar el Scheduler
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            scheduler.scheduleJob(job, trigger);

            System.out.println("Scheduler configurado. Job programado cada 10Min (lunes a sábado).");

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
