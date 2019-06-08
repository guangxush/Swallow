package com.shgx.config.controller;

import com.shgx.config.model.ApiResponse;
import com.shgx.config.model.Config;
import com.shgx.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author: guangxush
 * @create: 2019/06/08
 */
@RestController
@RequestMapping("/manage")
public class ConfigController {
    @Autowired
    private ConfigService configService;

    /**
     * 配置查询
     * @param key
     * @return
     */
    @RequestMapping(path = "/search/{key}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResponse<String> search(@PathVariable("key") String key){
        String value = configService.query(key);
        if(value!=null){
            return new ApiResponse<String>().success(value);
        } else{
            return new ApiResponse<String>().fail(null);
        }
    }

    /**
     * 配置保存
     * @param config
     * @return
     */
    @RequestMapping(path = "/save", method = RequestMethod.POST)
    @ResponseBody
    public ApiResponse<Boolean> save(@RequestBody Config config){
        Boolean result = configService.save(config);
        if(result){
            return new ApiResponse<Boolean>().success(true);
        } else{
            return new ApiResponse<Boolean>().fail(false);
        }
    }
}
