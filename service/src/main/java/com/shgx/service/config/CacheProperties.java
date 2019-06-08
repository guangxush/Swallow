package com.shgx.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
@ConfigurationProperties(prefix = "caffeine.config")
@Data
public class CacheProperties {
    private int initialCapacity;

    private long maximumSize;

    private long maximumWeight;

    private long expireAfterWriteNanos;

    private long expireAfterAccessNanos;

    private long refreshNanos;
}
