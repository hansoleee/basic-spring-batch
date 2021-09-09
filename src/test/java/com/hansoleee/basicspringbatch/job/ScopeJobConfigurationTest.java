package com.hansoleee.basicspringbatch.job;

import com.hansoleee.basicspringbatch.TestBatchConfig;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ScopeJobConfiguration.class, TestBatchConfig.class})
public class ScopeJobConfigurationTest {

    @Autowired
    public JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void jobParameter정상출력() throws Exception {
        //given
        LocalDate requestDate = LocalDate.of(2021, 8, 28);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("requestDate", requestDate.toString())
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        Assertions.assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}