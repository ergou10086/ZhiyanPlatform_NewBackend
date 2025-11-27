package hbnu.project.zhiyanbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ZhiyanBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhiyanBackendApplication.class, args);
    }

}
