package com.shgx.service.service;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
public interface ConfigService {

    /**
     * 查询数据库中key对应的value
     * @param key
     * @return
     */
    String query(String key);
}
