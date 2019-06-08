package com.shgx.service.repository;

import com.shgx.service.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/**
 * @author: guangxush
 * @create: 2019/06/08
 */
public interface ConfigRepo extends JpaRepository<Config, Long> {
    Optional<Config> findByKey(String key);
}
