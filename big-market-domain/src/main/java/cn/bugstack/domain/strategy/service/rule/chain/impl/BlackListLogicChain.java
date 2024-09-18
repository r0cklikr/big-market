package cn.bugstack.domain.strategy.service.rule.chain.impl;

import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

//责任链节点
@Slf4j
@Component("rule_blacklist")
public class BlackListLogicChain extends AbstractLogicChain {

    @Resource
    private IStrategyRepository repository;
    @Override
    //逻辑实现
    public Integer logic(String userId, Long strategyId) {
        log.info("抽奖责任链-黑名单开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        //数据库查询当前策略id所拥有黑名单value,只需要strategyid和rule_model
        String ruleValue = repository.queryStrategyRuleValueByStrategyIdAndRuleModel(strategyId,ruleModel());
        String[] split = ruleValue.split(":");
        //100:user001,user002
        Integer awardId = Integer.valueOf(split[0]);
        String[] userIdsInBlackList = split[1].split(",");
        for(String r : userIdsInBlackList){
            if(userId.equals(r)){
                //就在黑名单,直接返回这个奖品Id
                log.info("抽奖责任链-黑名单接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
                return awardId;
            }

        }
        //不是黑名单，走下一个节点
        log.info("抽奖责任链-黑名单放行 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        return next().logic(userId, strategyId);
    }

    @Override
    protected String ruleModel() {
        return "rule_blacklist";
    }
}
