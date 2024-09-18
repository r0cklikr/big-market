package cn.bugstack.domain.strategy.service.rule.chain.impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component("rule_weight")
@Slf4j
public class RuleWeightLogicChain extends AbstractLogicChain {
    @Resource
    private IStrategyRepository repository;
    @Resource
    private IStrategyDispatch strategyDispatch;
    public Long userScore = 1000L;

    /**
     * 权重规则过滤；
     * 1. 权重规则格式；4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
     * 2. 解析数据格式；判断哪个范围符合用户的特定抽奖范围
     *
     */
    @Override
    public Integer logic(String userId, Long strategyId) {
        //根据条件查询value值,这种规则具体到策略不是奖品
        String ruleValue = repository.queryStrategyRuleValueByStrategyIdAndRuleModel(strategyId,ruleModel());
        //空校验
        if(StrUtil.isBlank(ruleValue)){

        }
        //将value拆成以score为key,5000:101,102为value的map
        Map<Long, String> map = getAnalyticalValue(ruleValue);
        //匹配
        //排序
        ArrayList<Long> longs = new ArrayList<>(map.keySet());
        //Collections.sort(longs);
        //int p=0;
        Long point=null;
        //stream解决
        point = longs.stream().filter(r->userScore>=r).sorted((l,r)->-l.compareTo(r)).findFirst().orElse(null);

        if(point!=null){//找到了
            //去抽
            String key=map.get(point);
            Integer randomAwardId = strategyDispatch.getRandomAwardId(strategyId, key);

            log.info("抽奖责任链-权重接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), randomAwardId);

            return  randomAwardId;
        }
        //放行
        log.info("抽奖责任链-权重放行 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        return next().logic(userId, strategyId);

    }
    //将value拆成以score为key,5000:101,102为value的map
    private Map<Long, String> getAnalyticalValue(String ruleValue) {
        String[] split = ruleValue.split(" ");
        Map<Long,String> map = new HashMap<>();
        for(String s:split){
            String[] args = s.split(":");
            map.put(Long.valueOf(args[0]),s);
        }
        return map;
    }
    @Override
    protected String ruleModel() {
        return "rule_weight";
    }
}
