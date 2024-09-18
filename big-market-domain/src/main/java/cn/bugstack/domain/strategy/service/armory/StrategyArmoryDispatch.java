package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 策略装配库(兵工厂)，负责初始化策略计算
 * @create 2023-12-23 10:02
 */
@Slf4j
@Service
public class StrategyArmoryDispatch implements IStrategyArmory, IStrategyDispatch {

    @Resource
    private IStrategyRepository repository;

    @Override
    //生成两套随机表，一个是所有的奖品，一个是分段奖品
    public void assembleLotteryStrategy(Long strategyId) {
        // 1. 查询策略配置,根据传入的策略id查询拥有的策略-奖品列表
        List<StrategyAwardEntity> strategyAwardEntities = repository.queryStrategyAwardList(strategyId);
        assembleLotteryStrategy(strategyId.toString(),strategyAwardEntities);//可能没有分段，全部加进去
        //缓存获取strategy
        StrategyEntity strategyEntity= repository.queryStrategyEntityByStrategyId(strategyId);
        String ruleModel = strategyEntity.getRuleWeight();//这个分段rule可能没有
        if(ruleModel==null){
            //该策略没有分段rule
            return;
        }
        //根据strategyid和ruleModel去查，但是一个strategy就只有一个rule_weight,其实直接拿rule_weight去查就行
        //这里没有使用缓存
        StrategyRuleEntity strategyRuleEntity=repository.queryStrategyRuleByStrategyIdAndWeight(strategyId,ruleModel);
        //strategy有但是这里没有，直接一异常
        if(strategyRuleEntity==null){
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(),ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());

        }
        //因为数据库加了notnull,那么此时一定有value
        //将value拆成以5000:101，102为key,值为101,102这种map,来加redis
        Map<String, List<Integer>> map = strategyRuleEntity.getRuleWeightValues();

        Set<String> set = map.keySet();
        for(String key:set){
            List<Integer> list = map.get(key);
            //过滤最开始获得的strategyAwardEntities,就是list中有的才保留，然后进行随机处理
            List<StrategyAwardEntity> collect = strategyAwardEntities.stream().filter(r -> list.contains(r.getAwardId())).collect(Collectors.toList());
            assembleLotteryStrategy(strategyId.toString().concat("_").concat(key),collect);

        }

    }

    private void assembleLotteryStrategy(String key,List<StrategyAwardEntity> strategyAwardEntities){
        //比如有3个奖品,0.2,0.2,0.6
        //进行散列:  1 2 3 3 3//对应奖品id
        //          1 2 3 4 5//随机数

        //redis中需要存3个东西，1个是范围(总和/最小),1个是基于(1-范围的)为hashkey的hash表(快速根据随机值获取抽中的奖品)
        //还有一个

        //求最小
        BigDecimal minRate = strategyAwardEntities.stream().map(r -> r.getAwardRate()).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        //求总和
        BigDecimal totalRate = strategyAwardEntities.stream().map(r -> r.getAwardRate()).reduce(BigDecimal.ZERO,BigDecimal::add);

        //范围
        //0.13,0.17,0.2,0.5
        //range = total/min向上取整
        //还原value*range向上取整
        BigDecimal range = totalRate.divide(minRate, 0, RoundingMode.CEILING);

        //获取List
        List<Integer> list=new ArrayList<>();
        for(StrategyAwardEntity strategyAwardEntity : strategyAwardEntities) {
            for(int i=0;i<strategyAwardEntity.getAwardRate().multiply(range).intValue();i++){
                list.add(strategyAwardEntity.getAwardId());
            }
        }
        //乱序
        Collections.shuffle(list);

        //存hashmap
        HashMap<Integer, Integer> strategyAwardEntityHashMap = new HashMap<>();
        for(int i=0;i<list.size();i++){
            strategyAwardEntityHashMap.put(i,list.get(i));
        }
        //int p=1;
        //for (StrategyAwardEntity strategyAwardEntity : strategyAwardEntities) {
        //    for(int i=0;i<strategyAwardEntity.getAwardRate().multiply(range).intValue();i++){
        //        strategyAwardEntityHashMap.put(p++,strategyAwardEntity.getAwardId());
        //    }
        // }

        //通过随机数作为key快速获得value,使用hash
        //散列成数组，那么随机数范围就是数组大小不是range
        //0.5 0.1 0.01就会导致range>size()

        repository.storeStrategyAwardSearchRateTable(key,list.size(),strategyAwardEntityHashMap);

    }

    @Override
    public Integer getRandomAwardId(Long strategyId) {//直接抽奖
        int range= repository.getRateRange(strategyId);
        return repository.getStrategyAwardAssemble(strategyId.toString(),new SecureRandom().nextInt(range));
    }

    @Override
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
        //带有分段的抽奖
        String key=strategyId.toString().concat("_").concat(ruleWeightValue);

        int range= repository.getRateRange(key);
        return repository.getStrategyAwardAssemble(key,new SecureRandom().nextInt(range));
    }

}
