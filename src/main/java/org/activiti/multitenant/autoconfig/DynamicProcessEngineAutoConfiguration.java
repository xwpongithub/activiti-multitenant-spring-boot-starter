package org.activiti.multitenant.autoconfig;

import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.activiti.multitenant.core.DynamicRoutingProcessEngine;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(DynamicProcessEngineProperties.class)
@ConditionalOnProperty(prefix = DynamicProcessEngineProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfiguration(before = DynamicDataSourceProvider.class)
public class DynamicProcessEngineAutoConfiguration implements InitializingBean {

    @Bean
    DynamicRoutingProcessEngine processEngines() {
        System.out.println("++");
        DynamicRoutingProcessEngine dynamicRoutingProcessEngine = new DynamicRoutingProcessEngine();
//        dataSource.setStrategy(properties.getStrategy());
        return dynamicRoutingProcessEngine;
    }

//    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
//    @Bean
//    @ConditionalOnProperty(prefix = DynamicDataSourceProperties.PREFIX + ".aop", name = "enabled", havingValue = "true", matchIfMissing = true)
//    Advisor dynamicDatasourceAnnotationAdvisor(DsProcessor dsProcessor) {
//        DynamicDatasourceAopProperties aopProperties = properties.getAop();
//        DynamicDataSourceAnnotationInterceptor interceptor = new DynamicDataSourceAnnotationInterceptor(aopProperties.getAllowedPublicOnly(), dsProcessor);
//        DynamicDataSourceAnnotationAdvisor advisor = new DynamicDataSourceAnnotationAdvisor(interceptor, DS.class);
//        advisor.setOrder(aopProperties.getOrder());
//        return advisor;
//    }

    @Override
    public void afterPropertiesSet() throws Exception {
//        if (!CollectionUtils.isEmpty(dataSourcePropertiesCustomizers)) {
//            for (DynamicDataSourcePropertiesCustomizer customizer : dataSourcePropertiesCustomizers) {
//                customizer.customize(properties);
//            }
//        }
    }

}
