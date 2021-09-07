package com.hansoleee.basicspringbatch.job;

import com.hansoleee.basicspringbatch.entity.ClassInformation;
import com.hansoleee.basicspringbatch.entity.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TxWriterJobConfiguration {

    public static final String JOB_NAME = "txWriterJob";
    public static final String PREFIX_BEAN = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;

    @Value("${chunkSize:1000}")
    private int chunkSize;

    @Bean(JOB_NAME)
    public Job job() {
        return jobBuilderFactory.get(JOB_NAME)
                .preventRestart()
                .start(step())
                .build();
    }

    @Bean(PREFIX_BEAN + "step")
    public Step step() {
        return stepBuilderFactory.get(PREFIX_BEAN + "step")
                .<Teacher, Teacher>chunk(chunkSize)
                .reader(reader())
                .writer(writer())
                .build();
    }

    @Bean(PREFIX_BEAN + "reader")
    public JpaPagingItemReader<Teacher> reader() {
        return new JpaPagingItemReaderBuilder<Teacher>()
                .name(PREFIX_BEAN + "reader")
                .entityManagerFactory(emf)
                .pageSize(chunkSize)
                .queryString("SELECT t FROM Teacher t")
                .build();
    }

    public ItemWriter<Teacher> writer() {
        return items -> {
            log.info(">>>>> This is in writer()");
            for (Teacher item : items) {
                ClassInformation classInformation = new ClassInformation(item.getName(), item.getStudentList().size());
                log.info("반 정보={}", classInformation);
            }
        };
    }
}
