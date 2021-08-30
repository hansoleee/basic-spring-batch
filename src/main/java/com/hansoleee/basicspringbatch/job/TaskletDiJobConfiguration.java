package com.hansoleee.basicspringbatch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TaskletDiJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job taskletDiJob() {
        log.info(">>>>> definition taskletDiJob");
        return jobBuilderFactory.get("taskletDiJob")
                .start(taskletDiStep01())
                .next(taskletDiStep02(null))
                .build();
    }

    private final TaskletDiJobJobTasklet tasklet1;

    @Bean
    public Step taskletDiStep01() {
        return stepBuilderFactory.get("taskletDiStep01")
                .tasklet(tasklet1)
                .build();
    }

    @Bean
    @JobScope
    public Step taskletDiStep02(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("taskletDiStep02")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is taskletDiStep02");
                    log.info(">>>>> requestDate: {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
