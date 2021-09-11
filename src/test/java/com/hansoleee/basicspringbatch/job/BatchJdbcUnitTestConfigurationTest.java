package com.hansoleee.basicspringbatch.job;

import com.hansoleee.basicspringbatch.entity.SalesSum;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.sql.DataSource;
import java.time.LocalDate;

import static com.hansoleee.basicspringbatch.job.BatchJdbcUnitTestConfiguration.FORMATTER;

@RunWith(SpringRunner.class)
@SpringBatchTest
@EnableBatchProcessing // (1)
@TestExecutionListeners({ // (2)
        DependencyInjectionTestExecutionListener.class,
        StepScopeTestExecutionListener.class
})
@ContextConfiguration(classes = { // (3)
        BatchJdbcUnitTestConfiguration.class,
        BatchJdbcUnitTestConfigurationTest.TestDataSourceConfiguration.class
})
class BatchJdbcUnitTestConfigurationTest {

    @Autowired
    private JdbcPagingItemReader<SalesSum> reader;
    @Autowired
    private DataSource dataSource;

    private JdbcOperations jdbcTemplate;
    private LocalDate orderDate = LocalDate.of(2021, 9, 11);

    // (4)
    public StepExecution getStepExecutionV2() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("orderDate", this.orderDate.format(FORMATTER))
                .toJobParameters();

        return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @BeforeEach // (5)
    public void setUp() throws Exception {
        this.reader.setDataSource(this.dataSource);
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
    }

    @AfterEach // (6)
    public void tearDown() throws Exception {
        this.jdbcTemplate.update("delete from `sales`");
    }

    @Test
    void 기간내_Sales가_집계되어_SalesSum이_된다() throws Exception {
        //given
        long amount1 = 1000;
        long amount2 = 500;
        long amount3 = 100;

        saveSales(amount1, "1");
        saveSales(amount2, "2");
        saveSales(amount3, "3");

        Assertions.assertThat(reader.read().getAmountSum()).isEqualTo(amount1 + amount2 + amount3);
        Assertions.assertThat(reader.read()).isEqualTo(null);
    }

    private void saveSales(long amount, String orderNo) {
        jdbcTemplate.update("insert into `sales` (order_date, amount, order_no) values (?, ?, ?)", this.orderDate, amount, orderNo);
    }

    // (7)
    @Configuration
    static class TestDataSourceConfiguration {
        public static final String CREATE_SQL =
                "create table IF NOT EXISTS `sales` (" +
                        "id bigint not null auto_increment primary key, " +
                        "amount bigint not null, " +
                        "order_date date, " +
                        "order_no varchar(255)" +
                        ");";

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseFactory databaseFactory = new EmbeddedDatabaseFactory();
            databaseFactory.setDatabaseType(EmbeddedDatabaseType.H2);
            return databaseFactory.getDatabase();
        }

        @Bean
        public DataSourceInitializer initializer(DataSource dataSource) {
            DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
            dataSourceInitializer.setDataSource(dataSource);

            Resource create = new ByteArrayResource(CREATE_SQL.getBytes());
            dataSourceInitializer.setDatabasePopulator(new ResourceDatabasePopulator(create));

            return dataSourceInitializer;
        }
    }
}