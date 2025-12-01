package hbnu.project.zhiyanbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 项目启动主类
 *
 * @author ErgouTree
 */
@EnableJpaAuditing
@SpringBootApplication
public class ZhiyanBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZhiyanBackendApplication.class, args);
        System.out.println("开了？我说的是服务");
    }
}
