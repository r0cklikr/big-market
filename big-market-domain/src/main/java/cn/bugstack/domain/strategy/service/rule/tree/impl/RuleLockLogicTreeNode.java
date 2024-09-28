package cn.bugstack.domain.strategy.service.rule.tree.impl;

import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

//次数锁节点
@Slf4j
@Component("rule_lock")
//处理器
public class RuleLockLogicTreeNode implements ILogicTreeNode {
    @Resource
    private IActivityRepository activityRepository;
    // 用户抽奖次数，后续完成这部分流程开发的时候，从数据库/Redis中读取
    private Long userRaffleCount = 0L;

    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Integer awardId,String ruleValue,Long activityId) {
        //TODO 只有有rule_lock的奖品才需要走这个否则直接放行
        log.info("规则过滤-次数锁 userId:{} strategyId:{} awardId:{}", userId, strategyId, awardId);
        //多个活动可以绑定同一个策略，那么找到这个活动就需要根据活动id来找
        long raffleCount = 0L;
        if(activityId==null){
            raffleCount = 0l;
            log.info("抽奖时，活动id为null,设置默认抽奖次数为0,userId：{},strategyId:{},awardId:{}", userId, strategyId, awardId);
        }
        //查询抽奖次数
        raffleCount = activityRepository.getRaffleTimesByUserIdAndActivityId(userId,activityId);

        try {
            raffleCount = Long.parseLong(ruleValue);
        } catch (Exception e) {
            throw new RuntimeException("规则过滤-次数锁异常 ruleValue: " + ruleValue + " 配置不正确");
        }

        // 用户抽奖次数大于规则限定值，规则放行
        if (userRaffleCount >= raffleCount) {
            return DefaultTreeFactory.TreeActionEntity.builder()
                    .ruleLogicCheckType(RuleLogicCheckTypeVO.ALLOW)
                    .build();
        }

        // 用户抽奖次数小于规则限定值，规则拦截
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckType(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();

    }
}
