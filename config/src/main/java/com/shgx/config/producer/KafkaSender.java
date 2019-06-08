package com.shgx.config.producer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shgx.config.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
@Component
@Slf4j
@PropertySource("classpath:config/kafka.properties")
public class KafkaSender {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private Gson gson = new GsonBuilder().create();

    @Value("${kafka.topic}")
    private String topic;

    public void send(String key, String value) {
        Message message = new Message();
        message.setId(System.currentTimeMillis());
        message.setKey(key);
        message.setValue(value);
        message.setSendTime(new Date().toString());
        log.info("+++++++++++++++++++++  message = {}", gson.toJson(message));
        kafkaTemplate.send(topic, key, gson.toJson(message));
    }
}
