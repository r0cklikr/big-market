package cn.bugstack.test.domain;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.domain.strategy.service.rule.chain.impl.RuleWeightLogicChain;
import cn.bugstack.types.common.Constants;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽奖策略测试
 * @create 2024-01-06 13:28
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleStrategyTest {

    @Resource
    private IStrategyArmory strategyArmory;
    @Resource
    private IRaffleStrategy raffleStrategy;
    @Resource
    private RuleWeightLogicChain ruleWeightLogicChain;

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    RedissonClient redissonClient;
    //@Before
    public void setUp() {
        // 策略装配 100001、100002、100003
        //log.info("测试结果：{}", strategyArmory.assembleLotteryStrategy(100001L));
        //log.info("测试结果：{}", strategyArmory.assembleLotteryStrategy(100006L));

        // 通过反射 mock 规则中的值
        ReflectionTestUtils.setField(ruleWeightLogicChain, "userScore", 4900L);

    }


    @Test
    public void reds() throws InterruptedException {
        String cacheKey = "test2";
        RBlockingQueue<Integer> blockingQueue = redissonClient.getBlockingQueue(cacheKey);
        RDelayedQueue<Integer> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
       // delayedQueue.offer(1123, 30, TimeUnit.SECONDS);
       // delayedQueue.offer(23430,10, TimeUnit.SECONDS);

       //System.out.println( redissonClient.<Integer>getBlockingQueue(cacheKey).take());
        //固定搭配哈哈
        //RBlockingQueue<Integer> blockingQueue = redissonClient.getBlockingQueue(cacheKey);
        //RDelayedQueue<Integer> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        //delayedQueue.offer(1123, 30, TimeUnit.SECONDS);
        //delayedQueue.offer(23430,10, TimeUnit.SECONDS);
//        redissonClient.getBucket("bas").delete();
//        RMap<Object, Object> map = redissonClient.getMap("bas");
//        map.put("123",123);
//        //map.remove("123");
//        //RBucket<Object> bucket = redissonClient.getBucket("bas");
//
//        System.out.println(redissonClient.getList("bas").isExists());
//        System.out.println(redissonClient.getMap("bas").isExists());
//        System.out.println(redissonClient.getMap("bas").get("123"));
//        //RBucket<Object> bucket = redissonClient.getBucket("helllo");
//        //bucket.set("asd");


    }
    @Test
    public void redis() throws InterruptedException {

    }

    @Test
    public void test_performRaffle() throws InterruptedException {
        RaffleFactorEntity raffleFactorEntity = RaffleFactorEntity.builder()
                .userId("xiaofuge")
                .strategyId(100001L)
                .build();

        RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(raffleFactorEntity);

        log.info("请求参数：{}", JSON.toJSONString(raffleFactorEntity));
        log.info("测试结果：{}", JSON.toJSONString(raffleAwardEntity));
        new CountDownLatch(1).await();
    }

    @Test
    public void test_performRaffle_blacklist() {
        RaffleFactorEntity raffleFactorEntity = RaffleFactorEntity.builder()
                .userId("user003")  // 黑名单用户 user001,user002,user003
                .strategyId(100001L)
                .build();

        RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(raffleFactorEntity);

        log.info("请求参数：{}", JSON.toJSONString(raffleFactorEntity));
        log.info("测试结果：{}", JSON.toJSONString(raffleAwardEntity));
    }

    /**
     * 次数错校验，抽奖n次后解锁。100003 策略，你可以通过调整 @Before 的 setUp 方法中个人抽奖次数来验证。比如最开始设置0，之后设置10
     * ReflectionTestUtils.setField(ruleLockLogicFilter, "userRaffleCount", 10L);
     */
    @Test
    public void test_raffle_center_rule_lock(){
        RaffleFactorEntity raffleFactorEntity = RaffleFactorEntity.builder()
                .userId("xiaofuge")
                .strategyId(100001L)
                .build();

        RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(raffleFactorEntity);

        log.info("请求参数：{}", JSON.toJSONString(raffleFactorEntity));
        log.info("测试结果：{}", JSON.toJSONString(raffleAwardEntity));
    }

}
