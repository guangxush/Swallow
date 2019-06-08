package com.shgx.config.model;

import lombok.Data;


/**
 * @author: guangxush
 * @create: 2019/06/08
 */
@Data
public class Message {

    private Long id;

    private String key;

    private String value;

    private String sendTime;
}
