# 출처
[jojoldu님의 Spring Batch 가이드](https://jojoldu.tistory.com/325?category=902551)   
아래의 작성된 내용은 jojoldu 님의 글의 거의 모든 부분을 Copy 한 것임을 알려드립니다.
***

# 아주 간단한 실습 (Hello World)
***
```java
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
public class SimpleJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job simpleJob() {
        return jobBuilderFactory.get("simpleJob")
                .start(simpleStep01())
                .build();
    }
    
    @Bean
    public Step simpleStep01() {
        return stepBuilderFactory.get("simpleStep01")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Hello World, This is Step01.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}

```
- @Configuration
  - Spring Batch의 모든 job은 @configuration으로 등록해서 사용합니다.
- jobBuilderFactory.get("simpleJob")
  - simpleJob이란 이름의 Batch Job을 생성합니다.
  - job의 이름은 별도로 지정하지 않고, Builder를 이용해 지정합니다.
- stepBuilderFactory.get("simpleStep01")
  - simpleStep01이란 이름의 Batch Step을 생성합니다.
  - jobBuilderFactory.get("simpleJob")와 마찬가지로 Builder를 이용해 이름을 지정합니다.
- .tasklet((contribution, chunkContext))
  - Step 안에서 수행될 기능들을 명시합니다.
  - Tasklet은 Step 안에서 단일로 수행될 커스텀한 기능들을 선언할 때 사용합니다.
  - 지금은 Batch가 수행되면 log.info(">>>>> This is Step01")가 출력되도록 합니다.

<img width="100%" height="100%" src="./images/Job_n_Step.png"/> 

Job 안에는 여러 Step이 포함될 수 있습니다.   
Step은 2종류가 있습니다.   
1. Tasklet 으로 구성된 Step
2. Reader, Processor, Writer 묶음로 구성된 Step   
   
Step은 1번과 2번을 섞어서 구성할 수 없습니다.   
예를 들어, Reader와 Processor를 마치고 Tasklet으로 마무리하는 코드는 짤 수 없습니다.

위 Batch Job과 Tasklet을 이용한 코드를 실행해보겠습니다.
<img width="100%" height="100%" src="./images/배치실행결과01.png"/>
***

# 간단한 실습 (DB 연결)
***
## Spring Batch 실행을 위한 메타데이터 설명
Spring Batch를 사용하기위해서 메타데이터 테이블이 필요합니다.
>**메타데이터**란 **다른 데이터를 설명해 주는 데이터**입니다.   
>자세한 설명은 [위키백과의 메타데이터](https://ko.wikipedia.org/wiki/%EB%A9%94%ED%83%80%EB%8D%B0%EC%9D%B4%ED%84%B0) 또는 [나무위키의 메타데이터](https://namu.wiki/w/%EB%A9%94%ED%83%80%EB%8D%B0%EC%9D%B4%ED%84%B0) 을 참고해주세요.

Spring Batch의 메타데이터는 다음과 같은 내용을 담고 있습니다.
- 이전에 실행한 Job이 어떤 것들이 잇는지
- 최근 실패한 Batch Parameter가 어떤 것들이 있고, 성공한 Job은 어떤 것들이 있는지
- 다시 실행한다면 어디서부터 시작하면 될지
- 어떤 Job에 어떤 Step들이 있었고, Step들 중 성공한 Step과 실패한 Step들은 어떤 것들이 있는지

Batch 어플리케이션을 운영하기 위한 메타데이터가 여러 테이블에 나눠져 있습니다.   
메타데이터 테이블 구조는 아래와 같습니다.

<img width="100%" height="100%" src="./images/meta-data-erd.png"/>

출처: [metaDataSchema](https://docs.spring.io/spring-batch/docs/3.0.x/reference/html/metaDataSchema.html)

이 테이블들이 있어야만 Spring Batch가 정상 작동합니다. ~~아주 간단한 실습으로 돌린건 Batch가 아니란 말인가?~~   
H2 DB를 사용할 경우에는 해당 테이블을 Spring Boot가 자동으로 생성해주기 때문에 테이블을 생성하지 않았아도 에러가 없었습니다.   
하지만 MySQL 또는 PostgreSQL 이외의 다른 DB를 사용할 경우에는 개발자가 직접 생성해야만 합니다.   

해당 Table의 SQL은 Spring Batch에서 제공함으로 아래의 경로에서 사용하는 DBMS에 맞는 것으로 복붙하시면 됩니다.
<img src="images/metadata_sql_위치.png" width="100%" height="100%"/>

## DB 연결 (MariaDB 사용)
프로젝트의 src/main/resources/application.yml에 다음과 같이 Datasource 설정을 추가하겠습니다.
```yaml
spring:
  profiles:
    active: local
---
spring:
  profiles: local
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:tcp://localhost/~/h2-db/basic_spring_batch
    username: sa
    password:
---
spring:
  profiles: mariadb
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://localhost:3306/basic_spring_batch
    username: basicspringbatch
    password: basicspringbatch
```

설정을 마치셨다면 실행해봅니다.   

실행 환경을 application.yml에 작성된 Profile 중에서 mariadb 적용을 위한 설정을 해보겠습니다.
상단의 실행환경 버튼을 클릭합니다.
<img src="./images/배치실행방법01.png" width="100%" height="100%"/>

기본 실행 환경을 복사합니다.
<img src="./images/배치실행방법02.png" width="100%" height="100%"/>

복사된 실행 환경의 이름과 적용할 Profile 을 입력합니다.
<img src="./images/배치실행방법03.png" width="100%" height="100%"/>

생성한 실행 환경(mariadb)를 선택하고 Run 버튼을 눌러 실행합니다.
<img src="./images/배치실행방법04.p" : width=100%" height="100%"/>

이제 콘솔을 확인해보면 mariadb 로 실행되었다는 것을 확인할 수 있습니다.
<img src="./images/배치실행결과02.png" width="100%" height="100%"/>

콘솔을 좀 더 아래로 내려보시면
<img height="100%" src="./images/배치실행결과03.png" width="100%"/>

메타데이터 테이블인 BATCH_JOB_INSTANCE가 존재하지 않는다는 에러와 함께 배치 어플리케이션이 종료되었다는 것을 알 수 있습니다.

메타데이터 테이블은 위에 말씀드린 것처럼 schema-mysql.sql 파일을 검색하고 해당 SQL문을 사용하는 DB에서 실행해줍니다.   
![](images/배치실행결과04.png)

그리고 테이블이 잘 생성되었는지 확인해봅니다.
<img height="100%" src="./images/배치실행결과05.png" width="100%"/>

메타데이터 테이블을 생성한 뒤 다시 배치를 실행해보겠습니다.
<img height="100%" src="./images/배치실행결과06.png" width="100%"/>

MariaDB에서도 정상적으로 배치가 실행된 것을 확인할 수 있습니다.
***

# 메타데이터 테이블에 대한 학습
***
![](images/meta-data-erd.png)

위 메타데이터 테이블들의 역할이 무엇인지, 어떤 데이터를 저장하는 실습을 통해 하나씩 알아보겠습니다.

## BATCH_JOB_INSTANCE
가장 먼저 확인할 테이블은 BATCH_JOB_INSTANCE 입니다.   
![](images/BATCH_JOB_INSTANCE_조회결과01.png)

- JOB_INSTANCE_ID : BATCH_JOB_INSTANCE 테이블의 PK
- JOB_NAME : 수행한 Batch Job 이름

방금 실행한 simpleJob을 확인할 수 있습니다.   
BATCH_JOB_INSTANCE 테이블은 **Job Parameter에 따라 생성되는 테이블**입니다.   
Job Parameter란
Spring Batch가 실행될 때 **외부에서 받을 수 있는 Parameter**입니다.   

예를 들어,, 특정 날짜를 Job Parameter로 넘기면 **Spring Batch에서는 해당 날짜 데이터로 조회/가공/입력 등의 작업**을 할 수 있습니다.

같은 Batch Job이라도 Job Parameter가 다르면 BATCH_JOB_INSTANCE에는 기록되며,    
**Job Parameter가 같다면 기록되지 않습니다.**

확인해보겠습니다.
SimpleJobConfiguration 코드를 아래와 같이 수정합니다.
```java
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
public class SimpleJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job simpleJob() {
        return jobBuilderFactory.get("simpleJob")
                .start(simpleStep01(null))
                .build();
    }
    
    @Bean
    @JobScope
    public Step simpleStep01(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("simpleStep01")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Hello World, This is Step01.");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```
빨간색 부분만 추가/수정하였습니다.
![](images/JobParameter를매개변수로받는코드.png)

변경된 코드는 Job Parameter로 받은 값을 로그에 출력하는 기능을 추가하였습니다.

그럼 Job Parameter를 넣어서 Batch Application을 실행해보겠습니다.
좀 전과 마찬가지로 실행환경 설정창으로 이동합니다.   
![](images/JobParameter설정01.png)

아래와 같이 **Program arguments**에 requestDate=20210828을 입력합니다.   
![](images/JobParameter설정02.png)

프로그램을 실행봅니다.
그러면 전달된 Job Parameter가 로그에 찍힌 것을 확인할 수 있습니다.
![](images/JobParameter실행결과01.png)

BATCH_JOB_INSTANCE도 확인해보겠습니다.
![](images/JobParameter실행결과02.png)

새로운 JOB_INSTANCE_ID가 추가되었습니다.

그러면 Job Parameter가 같으면 어떻게 되는지도 확인해보겠습니다.   
(JOB_INSTANCE_ID는 Job Parameter에 따라 생성됩니다.)
![](images/JobParameter실행결과03.png)

실행 결과는 JobInstanceAlreadyCompleteException 예외와 메세지가 있습니다.
```text
A job instance already exists and is complete for parameters={requestDate=20210828}.  If you want to run this job again, change the parameters.
```
해당 Parameter에 대한 Job Instance가 존재하니 Parameter를 바꾸어 실행하라고 하네요.

이번에는 Parameter를 requestDate=20210810로 설정하고 실행해보겠습니다.
![](images/JobParameter변경실행결과01.png)

Parameter가 바뀌니 정상적으로 프로그램이 동작하였습니다.    
그리고 BATCH_JOB_INSTANCE 테이블에 JOB_INSTANCE_ID가 추가되었는지 확인해보겠습니다.
![](images/JobParameter변경실행결과02.png)

정리해보면, 동일한 Job은 Job Parameter가 달라지면 그때마다 BATCH_JOB_INSTANCE에 생성되며,   
동일한 Job Parameter를 통해 실행된 Job Instance는 여러개 존재할 수 없습니다.
***

## BATCH_JOB_EXECUTION
다음으로 알아볼 것은 BATCH_JOB_EXECUTION 테이블입니다.   
![](images/BATCH_JOB_EXECUTION_조회결과01.png)

BATCH_JOB_EXECUTION 테이블을 보시면 4개의 ROW가 있습니다.   
이전에 실행했던 파라미터가 없는 simpleJob 2개(JOB_INSTANCE_ID=1),   
~~위의 실습과정 상으로는 파라미터가 없는 simpleJob 실행 결과는 1개가 되어야합니다.    
저는 개인적인 호기심으로 2번 실행시켜 2개가 생겼습니다.~~    
requestDate=20210828 파라미터로 실행했던 simpleJob,   
requestDate=20210810 파라미터로 실행했던 simpleJob 까지 총 4개의 실행 데이터 입니다.

**BATCH_JOB_EXECUTION**과 **BATCH_JOB_INSTANCE**는 **부모-자식 관계**입니다.   
BATCH_JOB_EXECUTION은 자신의 부모 JOB_INSTANCE가 성공/실패했던 모든 내역을 가지고 있습니다.   
한 번 실습해보겠습니다.

SimpleJobConfiguration 코드를 아래와 같이 변경해보겠습니다.
```java
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

@Slf4j // log 사용을 위한 lombok Annotation
@Configuration
@RequiredArgsConstructor // 생성자 DI를 위한 lombok Annotation
public class SimpleJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job simpleJob() {
        return jobBuilderFactory.get("simpleJob")
                .start(simpleStep01(null))
                .next(simpleStep02(null))
                .build();
    }

    @Bean
    @JobScope
    public Step simpleStep01(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("simpleStep01")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Hello World, This is Step01.");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    @JobScope
    public Step simpleStep02(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("simpleStep02")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step02.");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```
아래와 같이 빨간색 부분만 추가/수정을 해주세요.   
![](images/배치실행실패코드01.png)

이번에는 Job Parameter를 requestDate=20210807로 변경하겠습니다.   
![](images/JobParameter설정03.png)

이제 실행해보겠습니다.   
![](images/배치실행실패결과01.png)
```text
java.lang.IllegalArgumentException: Step01에서 실패합니다.
```

아래로 조금 내려보시면   
![](images/배치실행실패결과02.png)
```text
Job: [SimpleJob: [name=simpleJob]] completed with the following parameters: [{requestDate=20210807}] and the following status: [FAILED] in 86ms
```

저희가 발생시킨 Exception과 함께 Batch Job이 실패했음을 볼 수 있습니다.   
그럼 BATCH_JOB_EXECUTION 테이블은 어떻게 변화되었는지 확인해보겠습니다.
![](images/배치실행실패결과03.png)
JOB_INSTANCE_ID를 FK로 물고있는 EXECUTION이 FAILED 라는 것과   
EXIT_MESSAGE로 Exception까지 저장되있는 것을 확인할 수 있습니다.

그럼 코드를 수정해서 JOB을 성공시켜보겠습니다.
```java
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
public class SimpleJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job simpleJob() {
        return jobBuilderFactory.get("simpleJob")
                .start(simpleStep01(null))
                .next(simpleStep02(null))
                .build();
    }

    @Bean
    @JobScope
    public Step simpleStep01(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("simpleStep01")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step01.");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    @JobScope
    public Step simpleStep02(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("simpleStep02")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is Step02.");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```
아래와 같이 simpleStep01에서 Exception 발생 부분을 로그 출력과 정상 종료 코드로 변경해주세요.
![](images/Job01nJob02배치실행코드01.png)

변경된 코드로 Batch를 실행해보겠습니다.
![](images/Job1nJob2배치실행결과01.png)
```text
Executing step: [simpleStep01]
>>>>> This is Step01.
>>>>> requestDate = 20210807
Step: [simpleStep01] executed in 31ms
Executing step: [simpleStep02]
>>>>> This is Step02.
>>>>> requestDate = 20210807
Step: [simpleStep02] executed in 18ms
```

JOB은 성공적으로 수행되었습니다.   
BATCH_JOB_EXECUTION 테이블을 확인해보겠습니다.   
![](images/Job1nJob2배치실행결과02.png)

노란색 박스의 JOB_INSTANCE_ID의 값이 4인 Row가 2개 있습니다.   
각 Row의 STATUS, EXIT_CODE Column을 보시면 FAILED와 COMPLETED가 있는 것을 확인할 수 있습니다.   
Job Parameter requestDate=20210807로 생성된 JOB_INSTANCE_ID(id=4)가 2번 실행되었고,   
첫번째는 실패, 두번째는 성공했다는 것을 알 수 있습니다.   

여기서 BATCH의 실행 조건은 
1. 동일한 Job Parameter로 2번 실행할 수 있다.
2. 동일한 Job Parameter로 성공한 기록이 있다면 재수행이 안된다.   

이라는 것을 알 수 있습니다.

그럼 여기까지의 내용을 정리해보겠습니다.   
위에서 나온 두 테이블(BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTIOIN)과  
저희가 만든 Spring Batch Job의 관계를 정리하면 아래와 같습니다.  
![](images/JOB,JOB_INSTANCE,JOB_EXXECUTION정리.png)

여기서 Job이란 저희가 SimpleJobConfiguration에 작성한 Spring Batch Job을 얘기합니다.

위 2개의 테이블 외에도 Job 관련된 테이블은 더 있습니다.  
예를들면 BATCH_JOB_EXECUTION_PARAM 테이블은 BATCH_JOB_EXECUTION 테이블이 생성될 당시에 입력받은 Job Parameter를 담고 있습니다.  
![](images/BATCH_JOB_EXECUTION_PARAMETER.png)

이외에도 다양한 메타데이터 테이블이 존재합니다.  
각각의 테이블은 앞으로 과정에서 필요할 때마다 추가적으로 소개드리겠습니다.  
~~각각의 테이블은 앞으로 과정을 학습하면서 추가적으로 작성하겠습니다.~~  
>JOB_EXECUTION 이외에도 STEP_EXECUTION 관련 테이블이 여러개 존재합니다.  
> 이 부분을 지금 다루기에는 내용이 너무 커지기 때문에 이후에 진행될 **Spring Batch 재시도/SKIP 전략**편에서 자세하게 소개드리겠습니다.

이후 과정에서는 Spring Batch 예제와 코드를 소개드리겠습니다.

### Spring Batch Test 코드는?
아래 내용은 위 글과 마찬가지로 jojoldu님의 원분을 그대로 옮겨적었습니다.  
글 첫 부분에 나오는 인칭 대명사의 '저'는 jojoldu님이라는 것을 미리 말씀드립니다.  
***
저의 이전 [Spring Batch](https://jojoldu.tistory.com/search/batch) 글을 보시면 아시겠지만, 저는 Spring Batch 예제를 항상 테스트 코드로 작성했습니다.
그러나 이 방식에 단점이 존재했는데요.
develop/production 환경에서 Spring Batch를 사용하시는 분들이 Batch Job Intstance Context 문제로 어려움을 겪는걸 많이 봤습니다.
Spring Batch에 적응하시기 전까지는 H2를 이용한 테스트 코드는 자제하시길 추천합니다.
H2를 이용한 테스트 코드는 최대한 나중에 보여드리겠습니다.
초반부에는 MySQL을 이용하면서 메타 테이블 정보가 남아 있는 상태에서의 Spring Batch에 최대한 적응하시도록 진행하겠습니다.
***

## Spring Batch Job Flow
Spring Batch의 Job은 하나 또는 여러 Step으로 구성할 수 있다고 말씀드렸습니다.  
Step은 **실제 Batch 작업을 수행하는 역할**을 합니다.  
실제로 Batch 비즈니스 로직을 처리하는 (ex: log.info()) 기능은 Step에 구현되어 있습니다.  
이처럼 Step에서는 **Batch로 실제 처리하고자 하는 기능과 설정을 모두 포함**하는 장소라고 생각하시면 됩니다.  

Batch 처리 내용을 담다보니, **Job 내부의 Step들 사이의 순서 또는 처리 흐름을 제어**할 필요가 생기게됩니다.  
이번엔 여러 Step들을 어떻게 관리할지에 대해서 알아보겠습니다.

### Next 
첫번째로 배워볼 것은 Next 입니다.

바로 샘플코드를 한번 작성해보겠습니다.
이번에 만들 Job은 StepNextJobConfiguration.java로 만들겠습니다.
```java
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
```
위 코드처럼 next()는 순차적으로 Step들 연결시킬때 사용됩니다.  
step01 -> step02 -> step03 순으로 하나씩 실행시킬 때 next()는 좋은 방법입니다.  

그럼 순차적으로 호출되는지 한번 실행해보겠습니다.  
이번에는 Job Parameter를 version=1로 변경하신 뒤  
![](images/StepNextJob_Parameter설정01.png)

실행 결과는 다음과 같습니다.
![](images/StepNextJob실행결과01.png)

이번에 새로 만든 stepNextJob 배치가 실행되었습니다.  
하지만 기존에 있던 simpleJob 배치도 실행되었습니다.

저희는 방금 만든 stepNextJob 배치만 실행하고 싶어집니다.  
그래서 **지정한 배치만 수행하도록** 설정은 변경해보겠습니다.

프로젝트의 src/main/resources/application.yml 에 아래의 코드를 추가합니다.  
```yaml
spring:
  profiles:
    active: local
  batch:
    job:
      names: ${job.name:NONE}
---
spring:
  profiles: local
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:tcp://localhost/~/h2-db/basic_spring_batch
    username: sa
    password:
---
spring:
  profiles: mariadb
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://localhost:3306/basic_spring_batch
    username: basicspringbatch
    password: basicspringbatch
```
![](images/StepNextJob실행설정01.png)

추가한 설정이 하는 일은 간단합니다.  
Spring Batch가 실행될 때 **Program arguments**로 **job.name 값**이 넘어오면 **해당 값과 일치하는 Job을 실행**합니다.  
여기서 ${job.name:NONE}을 보면 :을 사이에 두고 좌측에 job.name 우측에 NONE이 있습니다.  
이 코드는 Program arguments로 **job.name**이 있으면 **job.name 값을 할당**하고, 없으면 **NONE을 할당**하겠다는 의미입니다.  
중요한 것은 **spring.batch.job.name**에 **NONE**이 할당되면 **어떤 배치도 실행하지 않습니다**  
즉, 혹시라도 **값이 없을 때 모든 배치가 실행되지 않도록 막는 역할**입니다.

이제 Program arguments로 job.name을 설정하여 원하는 Job만 실행하겠습니다.
![](images/StepNextJob실행설정02.png)
Program arguments로 
```text
--job.name=stepNextJob 
```
입력합니다. (version=1은 이미 실행되었으니 version=2로 변경하셔야 합니다.)

실행해보겠습니다.
![](images/StepNextJob실행결과02.png)

지정한 stepNextJob만 수행하였습니다.  
앞으로 실무에서 사용할 때 수행하려는 job.name과 parameter를 바꿔서 배치를 실행시키면 되겠습니다.
> 실제 운영 환경에서는 java -jar batch-application.jar --job.name=simpleJob 과 같이 배치를 실행합니다.  
> ~~실제 노하우도 전달해 주시는 jojoldu님에게 매번 감동 spring batch에 대한 책 출간 빨리 해주세요~~

### 조건별 흐름 제어 (Flow)
Next가 순차적으로 Step의 순서를 제어한다는 것을 알았습니다.  
여기서 중요한 것은, **앞의 step에서 오류가 나면 나머지 뒤에 있는 step들은 실행되지 못한다**는 것 입니다.  

하지만 상황에 따라 Step의 수행 결과가 정상일 때는 Step B로,오류가 났을 때는 Step C로 수행해야할 때가 있습니다.  
![](images/StepNextConditionalJob_Flow01.png)

이럴 경우에 Spring Batch Job에서는 조건별로 Step을 사용할 수 있습니다.  
새로운 클래스 StepNextConditionalJobConfiguration.java 를 작성하고 살펴보겠습니다.
```java
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
    public Step step01() {
        return stepBuilderFactory.get("step01")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> This is stepNextConditionalJob Step01");

                    /**
                     * ExitStatus.FAILED로 지정
                     * 해당 Status 확인 후 다음 Flow를 진행
                     */
                    contribution.setExitStatus(ExitStatus.FAILED);
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
```
위 코드는 step01의 결과(성공/실패)에 따라 시나리오가 달라집니다.
- step01 실패 시나리오: step01 -> step03
- step01 성공 시나리오: step01 -> step02 -> step03

이런 전체 Flow를 관리하는 코드가 아래 코드입니다.
```java
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
```
- on()
  - 감지할 ExitStatus 지정
  - '*' 일 경우 모든 ExitStatus가 지정
- to()
  - 다음으로 이동할 Step 지정
- from()
  - 일종의 이벤트 리스너 역할
  - 상태 값을 보고 일치하는 상태라면 to()에 포함된 step을 호출
  - step01의 이벤트 캐치가 FAILED로 되있는 상태에서 추갛로 이벤트 감지하려면 from 사용
- end()
  - end는 FlowBuilder를 반환하는 end와 FlowBuilder를 종료하는 end 2개가 존재
  - on("*") 뒤에 있는 end는 FlowBuilder를 반환하는 end
  - build() 앞에 있는 end는 FlowBuilder를 종료하는 end
  - FlowBuilder를 반환하는 end 사용할 경우 계속해서 from 사용 가능

여기서 중요한 점은 on이 캐치하는 상태 값이 BatchStatus가 아닌 ExitStatus라는 점 입니다.  
그래서 분기처리를 위해 상태 값 조정이 필요하다면 ExitStatus를 조정해야합니다.  
조정하는 코드는 아래와 같습니다.  
![](images/StepNextConditionalJob_ExitStatus설정01.png)
```java
@Bean
public Step conditionalStep01() {
    return stepBuilderFactory.get("step01")
            .tasklet((contribution, chunkContext) -> {
                log.info(">>>>> This is stepNextConditionalJob Step01");

                /**
                 * ExitStatus.FAILED로 지정
                 * 해당 Status 확인 후 다음 Flow를 진행
                 */
                contribution.setExitStatus(ExitStatus.FAILED);
                
                return RepeatStatus.FINISHED;
            })
            .build();
}
```
본인이 원하는 상황에 따라 분기 로직을 작성하여 contribution.setExitStatus의 값을 변경하시면 됩니다.  
여기서는 먼저 FAILED를 발생하여 step01 -> step03 Flow 테스트를 해보겠습니다.

이제 실행해보겠습니다.  
**Program arguments의 job.name을 변경해 주세요.**
![](images/StepNextConditionalJob실행설정01.png)
```text
--job.name=stepNextConditionalJob version=1
```
version 값은 중복되지 않도록 설정해 주세요.

![](images/StepNextConditionalJob실행결과01.png)
**step01과 step03만 실행**된 것을 확인할 수 있습니다.  
ExitStatus.FAILED로 인해 step02가 실행되지 않았습니다.  

그럼 코드를 수정해서 step01 -> step02 -> step03이 되는지 확인해보겠습니다.
![](images/StepNextConditionalJob_실행흐름변경01.png)
```java
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
```
주석 처리하고 프로그램을 실행해보겠습니다.  
마찬가지로 실행을 위해 version을 변경해줍니다.  
![](images/StepNextConditionalJob실행설정02.png)
실행 결과를 보겠습니다.
![](images/StepNextConditionalJob실행결과02.png)

step01이 정상 종료되었고 이후 step02 -> step03 순서로 수행된 것을 확인할 수 있습니다.  
이 과정을 통해 조건별로 다른 step을 호출해야하는 로직도 쉽게 작성할 수 있게되었습니다.

### Batch Status vs Exit Status
위에서 나온 조건별 흐름 제어를 설명할 때 잠깐 언급했지만, **BatchStatus와 ExitStatus의 차이를 아는 것이 중요합니다.  

BatchStatus는 **Job 또는 Step의 실행 결과를 Spring에서 기록할 때 사용하는 Enum입니다.  
BatchStatus로 사용되는 값은 COMPLETED, STARTING, STARTED, STOPPING, STOPPED, FAILED, ABANDONED, UNKNOWN 값이 있습니다.  
대부분의 값들은 단어와 같은 뜻으로 해석하여 이해하시면 됩니다.  
![](images/BatchStatus01.png)

예를 들어,
```text
.on("FAILED").to(stepB())
```
위 코드에서 on 메소드가 참조하는 것은 BatchStatus으로 생각할 수 있지만 실제 참조되는 값은 Step의 ExitStatus입니다.  

ExitStatus는 **Step의 실행 후 상태**를 얘기합니다.  
![](images/ExitStatus01.png)

(ExitStatus는 Enum이 아닙니다.)

위 예제 (.on("FAILED").to(stepB()))를 풀이하면 **ExitStatus가 FAILED로 끝나게 되면 stepB를 실행하라**는 뜻입니다.  
Spring Batch는 **기본적으로 ExitStatus의 exitCode는 Step의 BatchStatus와 같도록 설정**되어 있습니다.  

만약 본인만의 exitCode가 필요한 경우 아래 예제를 통해 알아보겠습니다.(즉, BatchStatus와 ExitStatus가 다른 경우)
```text
.start(step01())
    .on("FAILED")
    .end()
.from(step01())
    .on("COMPLETED WITH SKIPS")
    .to(errorPrint01())
    .end()
.from(step01())
    .on("*")
    .to(step02())
    .end()
```
위 step01의 실행 결과는 아래의 3가지가 될 수 있습니다.
- step01이 실패하면, Job은 실패한다.
- step01이 성공적으로 수행되면, step02를 수행한다.
- step01이 ExitStatus를 COMPLETED WITH SKIPS로 종료하면, errorPrint01()을 수행한다.

위 코드에 나오는 COMPLETED WITH SKIPS는 ExitStatus에 없는 코드입니다.  
.on("COMPLETED WITH SKIPS")를 실행하기위해 COMPLETED WITH SKIPS exitCode를 반환하는 별도의 로직이 필요합니다.  

```java
public class SkipCheckingListener extends StepExecutionListenerSupport {
    public ExitStatus afterStep(StepExecution stepExecution) {
        String exitCode = stepExecution.getExitStatus().getExitCode();
        if (!exitCode.equals(ExitStatus.FAILED.getEixtCode()) && 
                stepExecution.getSkipCount() > 0) {
            return new ExitStatus("COMPLETED WITH SKIPS");
        } else {
            return null;
        }
    }
}
```
위 코드를 설명하면 StepExecutionListener에서는 먼저 Step이 성공적으로 수행되었는지 확인하고,  
**StepExecution의 skip 횟수가 0보다 클 경우 COPLETED WITH SKIPS의 exitCode를 갖는 ExitStatus를 반환**합니다.

### Decide
위 과정을 통해서 Step의 결과에 따라 서로 다른 Step으로 이동하는 방법을 알아보았습니다.  
이번에는 다른 방식의 분기 처리를 알아보겠습니다.  
위에서 작성한 코드는 2가지 문제가 있습니다.
1. Step이 담당하는 역할이 2개 이상이 됩니다.
   1. 실제 해당 Step이 처리해야할 로직 이외에도 분기 처리를 위해 ExitStatus 조작이 필요합니다. 
2. 다양한 분기 로직 처리의 어려움이 있습니다.
   1. ExitStatus를 커스텀하게 고치기 위해선 Listener를 생성하고 Job Flow에 등록하는 등 번거로움이 존재합니다.

명확하게 Step들 사이의 Flow 분기만 담당하면서 다양한 분기 처리가 가능한 타입이 있으면 좋겠습니다.  
그래서 Spring Batch에서는 Step들의 Flow속에서 **분기만 담당하는 타입**이 있습니다.  
JobExecutionDecider라고 하며, 이를 사용한 샘플 코드(DeciderJobConfiguration.java)를 한번 만들어보겠습니다.
```java
package com.hansoleee.basicspringbatch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeciderJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job deciderJob() {
        return jobBuilderFactory.get("deciderJob")
                .start(startStep())
                .next(decider())

                .from(decider())
                .on("ODD")
                .to(oddStep())

                .from(decider())
                .on("EVEN")
                .to(evenStep())

                .end()
                .build();
    }

    @Bean
    public Step startStep() {
        return stepBuilderFactory.get("startStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Start!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step evenStep() {
        return stepBuilderFactory.get("evenStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 짝수입니다.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step oddStep() {
        return stepBuilderFactory.get("oddStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 홀수입니다.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return new OddDecider();
    }

    public static class OddDecider implements JobExecutionDecider {

        @Override
        public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
            Random random = new Random();

            int randomNumber = random.nextInt(50) + 1;
            log.info("랜덤 숫자: {}", randomNumber);

            if (randomNumber % 2 == 0) {
                return new FlowExecutionStatus("EVEN");
            } else {
                return new FlowExecutionStatus("ODD");
            }
        }
    }
}
```
이 Batch의 Flow는 다음과 같습니다.
1. startStep -> oddDecider에서 홀수인지 짝수인지 구분 -> oddStep or evenStep 진행

decider()를 Flow 사이에 넣은 로직은 아래와 같습니다.
```java
@Bean
public Job deciderJob() {
    return jobBuilderFactory.get("deciderJob")
            .start(startStep())
            .next(decider())

            .from(decider())
            .on("ODD")
            .to(oddStep())

            .from(decider())
            .on("EVEN")
            .to(evenStep())

            .end()
            .build();
}
```
- start() 
  - Job Flow의 첫번째 Step을 시작합니다.
- next()
  - startStep 이후에 decider를 실행합니다.
- from()
  - Event Listener 역할을 합니다.
  - decider의 상태 값을 보고 일치하는 상태라면 to()에 포함된 step을 호출합니다.

코드는 이전 **조건별 흐름 제어 (Flow)**와 비슷하여 이해하기 쉬울 것 같습니다.

코드를 보시면, 분기 로직에 대한 모든 일은 OddDecider가 전담하고 있습니다.  
아무리 복잡한 분기 로직이 필요하더라도 Step과는 명확히 **역할과 책임이 분리**하여 진행할 수 있게 되었습니다.  

그럼 Decider 구현체를 살펴보겠습니다.
![](images/DeciderJob_Decider구현체01.png)

JobExecutionDecider 인터페이스를 구현한 OddDecider입니다.

여기서는 랜덤 숫자를 생성하여 홀수/짝수인지에 따라 서로 다른 상태를 반환합니다.  
주의하실 것은 Step으로 처리하는게 아니기 때문에 ExitStatus가 아닌 FlowExecutionStatus로 상태를 관리합니다.  

아주 쉽게 EVEN, ODD라는 상태를 생성하여 반환하였고, 이를 from().on()에서 사용하는 것을 알 수 있습니다.  
그럼 실행해보겠습니다.
![](images/DeciderJob실행결과홀수01.png)

![](images/DeciderJob실행결과짝수01.png)

실행해보시면  홀수/짝수가 낭면서 서로 다른 step(oddStep, evenStep)이 실행되는 것을 확인할 수 있습니다.  
~~정말 운이 좋게 홀수, 짝수가 연속해서 나왔습니다~~

###### 다음 시간 예고 
###### Spring Batch의 가장 중요한 개념인 Scope에 대해서 진행 
***

