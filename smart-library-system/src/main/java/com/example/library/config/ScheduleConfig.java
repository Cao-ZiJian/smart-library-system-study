package com.example.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置，开启 Spring Task 调度
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
