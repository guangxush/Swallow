package com.shgx.config.service;

import com.shgx.config.model.Config;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
public interface ConfigService {

    /**
     * 保存/更新配置
     * @param config
     * @return
     */
    Boolean save(Config config);

    /**
     * 查询数据库中key对应的value
     * @param key
     * @return
     */
    String query(String key);
}
