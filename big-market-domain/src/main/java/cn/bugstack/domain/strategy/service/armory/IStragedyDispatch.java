package cn.bugstack.domain.strategy.service.armory;

//策略抽奖调度
public interface IStragedyDispatch {
    /**
     * 获取抽奖策略装配的随机结果
     *
     * @param strategyId 策略ID
     * @return 抽奖结果
     */
    Integer getRandomAwardId(Long strategyId);
}
