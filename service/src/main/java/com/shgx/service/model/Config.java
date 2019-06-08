package com.shgx.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "config")
public class Config {
    /**
     * 自增id
     */
    @Id
    @Column(name = "config_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置名称
     */
    @Column(name = "config_name")
    private String key;

    /**
     * 配置值
     */
    @Column(name = "config_value")
    private String value;

    /**
     * 配置分支
     */
    @Column(name = "config_branch")
    private String branch;

    /**
     * 配置版本
     */
    @Column(name = "config_version")
    private String version;


    /**
     * 配置生效时间
     */
    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
}
