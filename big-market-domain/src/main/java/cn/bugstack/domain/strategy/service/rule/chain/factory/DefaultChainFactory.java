package cn.bugstack.domain.strategy.service.rule.chain.factory;

import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 责任链工厂
 * @create 2024-01-20 10:54
 */
@Service
public class DefaultChainFactory {

    private final Map<String, ILogicChain> logicChainGroup;
    protected IStrategyRepository repository;

    public DefaultChainFactory(Map<String, ILogicChain> logicChainGroup, IStrategyRepository repository) {
        this.logicChainGroup = logicChainGroup;
        this.repository = repository;
    }
    /**
     * 通过策略ID，构建责任链
     *
     * @param strategyId 策略ID
     * @return LogicChain
     */
    public ILogicChain openLogicChain(Long strategyId) {
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        String[] ruleModels = strategyEntity.getRuleModels();
        //空校验
        if(ruleModels == null || ruleModels.length == 0) {
             //用兜底节点
            return logicChainGroup.get("default");
        }
        //走责任链
        ILogicChain iLogicChain = logicChainGroup.get(ruleModels[0]);
        ILogicChain current=iLogicChain;
        //责任链的顺序与数据库rule_models顺序有关
        for(int i=1;i<ruleModels.length;i++) {
            ILogicChain nextChain = logicChainGroup.get(ruleModels[i]);
            current.setNext(nextChain);
            current=nextChain;

        }
       // current.addNext(logicChainGroup.get("default"));
        current.setNext(logicChainGroup.get("default"));
        return iLogicChain;
    }

}
