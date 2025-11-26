package hbnu.project.zhiyanbackend.auth.config;

import hbnu.project.zhiyanbackend.auth.model.converter.RoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 显式注册 MapStruct 生成的 RoleConverter，
 * 避免因组件扫描顺序导致的缺失。
 *
 * @author ErgouTree
 */
@Configuration
public class RoleConverterConfig {

    @Bean
    public RoleConverter roleConverter() {
        return RoleConverter.INSTANCE;
    }
}

