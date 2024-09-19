package cn.bugstack.domain.strategy.service.rule.chain;

public interface ILogicChain {

    Integer logic(String userId,Long strategyId);

    //ILogicChain addNext(ILogicChain next);
    void setNext(ILogicChain next);
    ILogicChain next();
}
