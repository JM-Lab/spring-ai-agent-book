다음은 Spring Boot로 작성된 간단한 자바 애플리케이션 예제입니다.

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

---

Spring Boot 애플리케이션은 Maven Wrapper로 실행할 수 있습니다.

```
./mvnw spring-boot:run
```
