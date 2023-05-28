package org.activiti.multitenant.autoconfig;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = DynamicProcessEngineProperties.PREFIX)
public class DynamicProcessEngineProperties {

    public static final String PREFIX = "spring.process-engine.dynamic";

}
