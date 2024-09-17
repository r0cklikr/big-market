package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.bugstack.infrastructure.persistent.po.StrategyAward;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import org.redisson.api.RMap;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 策略服务仓储实现
 * @create 2023-12-23 10:33
 */
@Repository
public class StrategyRepository implements IStrategyRepository {

    @Resource
    private IStrategyAwardDao strategyAwardDao;
    @Resource
    private IRedisService redisService;

    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        String key = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId;
        List<StrategyAwardEntity> strategyAwardEntities = redisService.getValue(key);
        if(!CollUtil.isEmpty(strategyAwardEntities)){
            return strategyAwardEntities;
        }
        //为空，从mysql查
        List<StrategyAward> strategyAwards = strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        if(CollUtil.isEmpty(strategyAwards)){
            throw new RuntimeException("没有该策略");
        }

        List<StrategyAwardEntity> awardEntities = BeanUtil.copyToList(strategyAwards, StrategyAwardEntity.class);
        redisService.setValue(key,awardEntities);

        return awardEntities;



    }

    @Override
    public void storeStrategyAwardSearchRateTable(Long strategyId, int range, HashMap<Integer, Integer> strategyAwardEntityHashMap) {
        //存该策略id的range
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+strategyId,range);

        //存hash
        RMap<Object, Object> map = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + strategyId);
        map.putAll(strategyAwardEntityHashMap);


    }



    @Override
    public Integer getStrategyAwardAssemble(Long strategyId, Integer rateKey) {
        RMap<Integer, Integer> map = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+strategyId);
        return map.get(rateKey);

       // return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+strategyId,rateKey);

    }

    @Override
    public int getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+strategyId.toString());

    }

}
