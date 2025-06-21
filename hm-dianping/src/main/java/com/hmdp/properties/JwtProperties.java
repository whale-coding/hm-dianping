package com.hmdp.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Classname: JwtProperties
 * @Date: 2025/6/21 12:51
 * @Author: njq
 * @Description:
 */
@Component
@ConfigurationProperties(prefix = "hmdp.jwt")
@Data
public class JwtProperties {
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;
}
