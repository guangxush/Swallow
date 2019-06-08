package com.shgx.service.service.impl;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.shgx.service.model.Config;
import com.shgx.service.repository.ConfigRepo;
import com.shgx.service.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.shgx.service.config.CacheConfig.DEVICE_CACHE;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private ConfigRepo configRepo;

    @Autowired
    @Qualifier(DEVICE_CACHE)
    private Caffeine<Object, Object> caffeineBuilder;

    private LoadingCache<String, String> cache;

    @PostConstruct
    public void initCache() {
        cache = caffeineBuilder.build(new CacheLoader<String, String>() {
            @Override
            public String load(String key) throws Exception {
                log.info("fail to hit cache for key={}, try to find it via RPC.", key);
                String value = queryValueFromDB(key);
                if (value == null) {
                    log.info("fail to hit cache for key={}, the value is null.", key);
                    return null;
                }
                return value;
            }

            @Override
            public String reload(String key, String oldValue)throws Exception{
                return oldValue;
            }
        });
    }

    public String queryValueFromDB(String key){
        Optional<Config> config = configRepo.findByKey(key);
        if(config.isPresent()){
            return config.get().getValue();
        }
        return null;
    }

    /**
     * 手动删除缓存
     * @param key
     */
    public void invalidateCache(String key){
        cache.refresh(key);
    }

    @Override
    public String query(String key) {
        if(key==null){
            log.error("the key is null!");
            return null;
        }
        String config = cache.get(key);
        return config;
    }
}
