package cn.bugstack.domain.strategy.service.rule.impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.entity.RuleMatterEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.annotation.LogicStrategy;
import cn.bugstack.domain.strategy.service.rule.ILogicFilter;
import cn.bugstack.domain.strategy.service.rule.factory.DefaultLogicFactory;
import cn.bugstack.types.common.Constants;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 【抽奖前规则】根据抽奖权重返回可抽奖范围KEY
 * @create 2024-01-06 09:57
 */
@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_WIGHT)
public class RuleWeightLogicFilter implements ILogicFilter<RuleActionEntity.RaffleBeforeEntity> {

    @Resource
    private IStrategyRepository repository;

    public Long userScore = 7000L;

    /**
     * 权重规则过滤；
     * 1. 权重规则格式；4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
     * 2. 解析数据格式；判断哪个范围符合用户的特定抽奖范围
     *
     * @param ruleMatterEntity 规则物料实体对象
     * @return 规则过滤结果
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-权重范围 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());

        String userId = ruleMatterEntity.getUserId();
        //根据条件查询value值
        String ruleValue = repository.queryStrategyRuleValue(ruleMatterEntity.getStrategyId(),ruleMatterEntity.getAwardId(),ruleMatterEntity.getRuleModel());

        //空校验
        if(StrUtil.isBlank(ruleValue)){
            RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }
        //将value拆成以score为key,5000:101,102为value的map
        Map<Long, String> map = getAnalyticalValue(ruleValue);
        //匹配
        //排序
        ArrayList<Long> longs = new ArrayList<>(map.keySet());
        Collections.sort(longs);
        //int p=0;
        Long point=null;
        //stream解决
        point = longs.stream().filter(r->userScore>=r).sorted((l,r)->-l.compareTo(r)).findFirst().orElse(null);
        if(point!=null){
            return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                    .data(RuleActionEntity.RaffleBeforeEntity.builder()
                            .strategyId(ruleMatterEntity.getStrategyId())
                            .ruleWeightValueKey(map.get(point))
                            .build())
                    .ruleModel(DefaultLogicFactory.LogicModel.RULE_WIGHT.getCode())
                    .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                    .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                    .build();
        }

        return RuleActionEntity.<RuleActionEntity.RaffleBeforeEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
       /* while(1==1){
            if(this.userScore.longValue()>=longs.get(p)){
                if(p==longs.size()-1){
                   point = longs.get(p);
                }else if(this.userScore.longValue()<longs.get(p+1)){
                    point = longs.get(p);
                }
            }else{
                p++;
                if(p==longs.size()){
                    break;
                }
            }
        }*/


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

}
