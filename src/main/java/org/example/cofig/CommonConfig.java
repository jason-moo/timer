package org.example.cofig;

import org.example.timer.QyyTimer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CommonConfig {

    @Bean
    public QyyTimer qyyTimer() {
        return new QyyTimer(64l, TimeUnit.SECONDS, 10);
    }

//        @Bean
//    public Asss asss() {
//        return new Asss(1);
//    }
}
