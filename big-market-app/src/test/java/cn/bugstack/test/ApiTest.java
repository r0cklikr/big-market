package cn.bugstack.test;

import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RMap;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 功能测试
 * @create 2023-12-23 11:39
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Resource
    private IRedisService redisService;
    @Test
    public void test() {
        RMap<Object, Object> map = redisService.getMap("hello");//没有就创建
        map.put(1,101);
        map.put(2,101);
        map.put(3,101);
        map.put(4,102);
        map.put(5,102);
        map.put(6,102);
        map.put(7,103);
        map.put(8,103);
        map.put(9,103);
        map.put(10,104);
        log.info(redisService.getFromMap("hello",3).toString());

    }

}
