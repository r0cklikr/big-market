package cn.bugstack.domain.strategy.service.rule.chain;

import java.lang.ref.PhantomReference;

public abstract class AbstractLogicChain implements ILogicChain {

    private ILogicChain next;


    @Override
    public ILogicChain addNext(ILogicChain next) {
        this.next=next;
        return next;
    }

    @Override
    public ILogicChain next() {
        return next;
    }

    protected abstract String ruleModel();
}
