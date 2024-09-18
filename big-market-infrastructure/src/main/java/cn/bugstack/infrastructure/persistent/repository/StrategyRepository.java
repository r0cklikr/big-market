package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.infrastructure.persistent.dao.IStrategyAwardDao;
import cn.bugstack.infrastructure.persistent.dao.IStrategyDao;
import cn.bugstack.infrastructure.persistent.dao.IStrategyRuleDao;
import cn.bugstack.infrastructure.persistent.po.Strategy;
import cn.bugstack.infrastructure.persistent.po.StrategyAward;
import cn.bugstack.infrastructure.persistent.po.StrategyRule;
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
    private IStrategyDao strategyDao;
    @Resource
    private IRedisService redisService;
    @Resource
    private IStrategyRuleDao strategyRuleDao;

    @Override
    //redis+mysql
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
    //redis
    //key=前缀strategyId_5000:102,103
    public void storeStrategyAwardSearchRateTable(String key, int range, HashMap<Integer, Integer> strategyAwardEntityHashMap) {
        //存该策略id的range
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key,range);

        //存hash
        RMap<Object, Object> map = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key);
        map.putAll(strategyAwardEntityHashMap);


    }



    @Override
    //redis
    public Integer getStrategyAwardAssemble(String key, Integer rateKey) {
        RMap<Integer, Integer> map = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+key);
        return map.get(rateKey);

       // return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+strategyId,rateKey);

    }

    @Override
    //根据Id查询strtegy
    //redis+mysql
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        String key= Constants.RedisKey.STRATEGY_KEY + strategyId;
        StrategyEntity strategyEntity =  redisService.getValue(key);
        //缓存中没有
        if(strategyEntity == null){
            Strategy strategy = strategyDao.queryStrategyByStrategyId(strategyId);//数据库返回的是po
            //放到缓存
            strategyEntity = BeanUtil.copyProperties(strategy, StrategyEntity.class);
            redisService.setValue(key,strategyEntity);
        }
        return strategyEntity;

    }

    @Override
    //mysql
    public StrategyRuleEntity queryStrategyRuleByStrategyIdAndWeight(Long strategyId,String ruleModel) {
        //从strategy_rule中查这个规则
        //封装查询参数
        StrategyRule strategyRuleQuery=new StrategyRule();
        strategyRuleQuery.setStrategyId(strategyId);
        strategyRuleQuery.setRuleModel(ruleModel);
        //数据库查
        StrategyRule strategyRule =  strategyRuleDao.queryStrategyRuleByStrategyIdAndWeight(strategyRuleQuery);
        //转换类型
        StrategyRuleEntity strategyRuleEntity = BeanUtil.copyProperties(strategyRule, StrategyRuleEntity.class);
        return strategyRuleEntity;

    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel) {
        StrategyRule strategyRule = new StrategyRule();
        strategyRule.setStrategyId(strategyId);
        strategyRule.setAwardId(awardId);
        strategyRule.setRuleModel(ruleModel);
        return strategyRuleDao.queryStrategyRuleValue(strategyRule);
    }

    @Override
    //redis
    public int getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+strategyId.toString());

    }
    @Override
    //redis
    public int getRateRange(String key) {
        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key.toString());

    }

    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId) {
        StrategyAward strategyAward = new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        String ruleModels = strategyAwardDao.queryStrategyAwardRuleModels(strategyAward);
        return StrategyAwardRuleModelVO.builder().ruleModels(ruleModels).build();
    }
}
