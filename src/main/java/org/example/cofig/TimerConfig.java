package org.example.cofig;

import org.example.task.Task;
import org.example.timer.QyyTimer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;


@Configuration
public class TimerConfig implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private QyyTimer qyyTimer;

    @Bean
    public QyyTimer qyyTimer() {
        return new QyyTimer(1, TimeUnit.SECONDS, 64);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        qyyTimer.start();
        qyyTimer.addTask(new Task("2131312"), 10, TimeUnit.SECONDS);
    }

}
