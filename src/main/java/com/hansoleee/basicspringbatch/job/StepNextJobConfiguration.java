package com.hansoleee.basicspringbatch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StepNextJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job stepNextJob() {
        return jobBuilderFactory.get("stepNextJob")
                .start(step01())
                .next(step02())
                .next(step03())
                .build();
    }

    @Bean
    public Step step01() {
        return stepBuilderFactory.get("step01")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step01");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step step02() {
        return stepBuilderFactory.get("step02")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step02");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step step03() {
        return stepBuilderFactory.get("step03")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step03");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
