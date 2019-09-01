## 1 需求介绍
使用Spring Boot开发微服务的过程中难免会遇到配置问题，常见的配置方式为：配置文件设置、数据库动态配置以及数据库加缓存进行配置等。
###  1.1 不同配置方式的优缺点分析
- 配置文件配置：通过读取properties中的配置，将代码中用到的属性抽取出来放入配置文件，代码中通过@Value注解读取属性的值，这样方便对配置进行维护和管理，有助于提高代码的可读性和可维护性。但是@Value在Spring初始化的时候对值进行读取，一旦服务创建好之后不能修改，如果想修改某一个配置，尤其是变化较为频繁的字段，需要重启整个服务，不方便。

![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow1.png)

- 读取数据库获得动态配置： 动态的读取数据库中的字段是一种非常有效的动态配置方式，后台只需要将相关的属性存储在数据库中，当数据库中的值修改之后，代码中会及时获取到新的属性值，从而完成动态配置。但是这种配置方式会频繁的读取数据库，如果每次查询操作都会用到数据库中的某个配置，会造成数据库热点问题，影响性能。

![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow2.png)

- 读取数据库+缓存获得动态配置：如果既要保证配置的动态性又要考虑性能，可以通过缓存的方式将硬盘中数据库中的属性值放在cache中（30分支定时从数据库中读取一次），从而减少磁盘的读取操作同时也提高了读取效率，但是对缓存的要求较高，如果某个配置需要保证实时性（紧急情况下对服务降级的配置），及时数据库中的值以及更新，但是缓存还没有更新，不能够保证数据一致性。需要在可用性和一致性中寻求一个平衡。

![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow3.png)

###  1.2 配置方式的改进
通过分析上述三种配置方式的优缺点，我们希望引入一个统一的配置中心，能够对不同分支，不同服务，不同代码进行动态配置，同时也要保证时效性和可用性。这里在数据库+缓存的基础上，引入消息中间件，如果管理员对代码配置进行修改，能够通过消息及时通知到服务，然后刷新缓存，获取修改后的消息（不过如果配置频繁修改，该方法还需要改进，但是可以保证缓存与数据库在修改之后的一致性）。

![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow4.png)

## 2 框架介绍
这里通过配置中心接口对数据库中的值进行修改，同时其他服务通过数据库+缓存的方式从数据库中读取配置，配置修改之后如何在不同服务之间进行通知，这里采用消息中间件进行通知，或者采用RPC的方式更快，其他服务Service1-3通过订阅不同配置的消息，如果有对数据库中配置的修改操作，会对订阅消息的服务发送请求，重新刷新缓存数据，旧的缓存失效，起到即时通知的作用。

![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow5.png)

## 3 代码介绍
### 3.1 config模块
承担配置中心的作用，用于数据库中配置的存储以及Kafka（这里采用kafka作为消息中间件）消息的通知。
- 3.1.1配置存储
```
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
```
- 3.1.2 消息发送
 ```
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
```
### 3.2 service模块
简单实现了一个基本的配置查询操作（这里使用Caffeine缓存），以及Kafka消息订阅。
- 3.2.1 添加缓存读取数据库中的值
```
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
                return load(key);
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
     * 手动刷新缓存
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
```
- 3.2.2 kafka消息订阅
```
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
```
## 4 效果分析
### 4.1 数据插入：
```
http://localhost:8081/config/manage/save
```
发送JSON数据
```
{
  "key":"hello",
  "value":"world",
  "branch":"dev",
  "version":"v1.0",
  "date":"2019-06-08"
}
```
数据库存储值，并通过kafka发送消息：

![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow6.png)

### 4.2 数据查询
```
http://localhost:8080/config/manage/search/hello
```
多次点击查询，但是只有一条SQL查询语句，数据以及保存在了缓存中。

![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow7.png)

 ### 4.3 配置更新
这时修改hello的值为world!!!
![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow8.png)
service模块收到消息通知并更新了缓存：
![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow9.png)
![](https://github.com/guangxush/iTechHeart/blob/master/image/Swallow/Swallow10.png)
![](https://upload-images.jianshu.io/upload_images/7632302-8258a006eabf90e0.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
## 5 完整源码
[github参考](https://github.com/guangxush/Swallow)

