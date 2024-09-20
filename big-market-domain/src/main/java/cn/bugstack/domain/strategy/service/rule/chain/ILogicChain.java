package cn.bugstack.domain.strategy.service.rule.chain;

import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultChainFactory;

public interface ILogicChain {

    DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId);

    //ILogicChain addNext(ILogicChain next);
    void setNext(ILogicChain next);
    ILogicChain next();
}
