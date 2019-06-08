package com.shgx.config.service.impl;

import com.shgx.config.model.Config;
import com.shgx.config.producer.KafkaSender;
import com.shgx.config.repository.ConfigRepo;
import com.shgx.config.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
    private KafkaSender sender;

    @Override
    public Boolean save(Config config) {
        if(config.getKey()==null||config.getValue()==null){
            log.error("the config is not enabled!");
            return false;
        }
        try{
            Optional<Config> configDB = configRepo.findByKey(config.getKey());
            if(configDB.isPresent()){
                Config configTemp = configDB.get();
                configTemp.setValue(config.getValue());
                configRepo.save(configTemp);
            }else{
                configRepo.save(config);
            }
            sender.send(config.getKey(), config.getValue());
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public String query(String key) {
        if(key==null){
            log.error("the key is null!");
            return null;
        }
        Optional<Config> config = configRepo.findByKey(key);
        if(config.isPresent()){
            return config.get().getValue();
        }
        return null;
    }

}
