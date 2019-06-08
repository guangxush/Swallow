package com.shgx.service.Consumer;

import com.alibaba.fastjson.JSONObject;
import com.shgx.service.model.Message;
import com.shgx.service.service.impl.ConfigServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
@Component
@Slf4j
@PropertySource("classpath:config/kafka.properties")
public class Listener {

    @Value("${kafka.topic}")
    private String topic;

    @Autowired
    private ConfigServiceImpl service;

    @KafkaListener(groupId = "group0", topics = "shgx")
    public void listener(ConsumerRecord<?, ?> cr) throws Exception {
        Message message = JSONObject.parseObject((String) cr.value(), Message.class);
        String key = (String) cr.key();
        String value = message.getValue();
        String oldValue = service.query(key);
        if(!oldValue.equals(value)){
            //原来的值已经更新，重新从数据库中读取缓存
            service.invalidateCache((String) cr.key());
        }
        log.info("+++++++++++++++++++++  topic = {}, key = {}, value = {}.", cr.topic(), cr.key(), cr.value());
    }
}
