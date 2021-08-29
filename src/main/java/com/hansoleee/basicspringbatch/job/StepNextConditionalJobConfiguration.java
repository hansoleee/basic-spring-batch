package com.hansoleee.basicspringbatch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
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
public class StepNextConditionalJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job stepNextConditionalJob() {
        return jobBuilderFactory.get("stepNextConditionalJob")

                .start(conditionalStep01())
                .on("FAILED") // FAILED 경우에
                .to(conditionalStep03()) // step03으로 이동
                .on("*") // step03 결과에 관계 없이
                .end() // step03으로 이동하면 Flow를 종료

                .from(conditionalStep01()) // step01로부터
                .on("*") // FAILED를 제외한 모든 경우
                .to(conditionalStep02()) // step02로 이동
                .next(conditionalStep03()) // step02가 정상 종료된다면 step03으로 이동
                .on("*") // step03 결과에 관계 없이
                .end() // step03으로 이동하면 Flow를 종료

                .end() // Job 종료
                .build();
    }

    @Bean
    public Step conditionalStep01() {
        return stepBuilderFactory.get("step01")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is stepNextConditionalJob Step01");

                    /**
                     * ExitStatus.FAILED로 지정
                     * 해당 Status 확인 후 다음 Flow를 진행
                     */
                    // contribution.setExitStatus(ExitStatus.FAILED);

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step conditionalStep02() {
        return stepBuilderFactory.get("conditionalStep02")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is stepNextConditionalJob Step02");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step conditionalStep03() {
        return stepBuilderFactory.get("conditionalStep03")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is stepNextConditionalJob Step03");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
