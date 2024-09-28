package cn.bugstack.infrastructure.persistent.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.*;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.infrastructure.persistent.dao.*;
import cn.bugstack.infrastructure.persistent.po.*;
import cn.bugstack.infrastructure.persistent.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.exception.AppException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.bugstack.types.enums.ResponseCode.UN_ASSEMBLED_STRATEGY_ARMORY;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 策略服务仓储实现
 * @create 2023-12-23 10:33
 */
@Repository
@Slf4j
public class StrategyRepository implements IStrategyRepository {

    @Resource
    private IStrategyAwardDao strategyAwardDao;
    @Resource
    private IRaffleActivityDao raffleActivityDao;
    @Resource
    private IStrategyDao strategyDao;
    @Resource
    private IRedisService redisService;
    @Resource
    private IStrategyRuleDao strategyRuleDao;
    @Resource
    private IRuleTreeDao ruleTreeDao;
    @Resource
    private IRuleTreeNodeDao ruleTreeNodeDao;
    @Resource
    private IRuleTreeNodeLineDao ruleTreeNodeLineDao;
    @Resource
    private RedissonClient redissonClient;
    @Override
    //redis+mysql
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        String key = Constants.RedisKey.STRATEGY_AWARD_LIST_KEY + strategyId;
        List<StrategyAwardEntity> strategyAwardEntities = redisService.getValue(key);
        if(!CollUtil.isEmpty(strategyAwardEntities)){
            return strategyAwardEntities;
        }
        //为空，从mysql查
        List<StrategyAward> strategyAwards = strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        if(CollUtil.isEmpty(strategyAwards)){
            throw new RuntimeException("没有该策略");
        }

        List<StrategyAwardEntity> awardEntities = BeanUtil.copyToList(strategyAwards, StrategyAwardEntity.class);
        redisService.setValue(key,awardEntities);

        return awardEntities;



    }

    @Override
    //redis
    //key=前缀strategyId_5000:102,103
    public void storeStrategyAwardSearchRateTable(String key, int range, HashMap<Integer, Integer> strategyAwardEntityHashMap) {
        //存该策略id的range
        redisService.setValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key,range);

        //存hash
        RMap<Object, Object> map = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key);
        map.putAll(strategyAwardEntityHashMap);


    }



    @Override
    //redis
    public Integer getStrategyAwardAssemble(String key, Integer rateKey) {
        RMap<Integer, Integer> map = redisService.getMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+key);
        return map.get(rateKey);

       // return redisService.getFromMap(Constants.RedisKey.STRATEGY_RATE_TABLE_KEY+strategyId,rateKey);

    }

    @Override
    //根据Id查询strtegy
    //redis+mysql
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        String key= Constants.RedisKey.STRATEGY_KEY + strategyId;
        StrategyEntity strategyEntity =  redisService.getValue(key);
        //缓存中没有
        if(strategyEntity == null){
            Strategy strategy = strategyDao.queryStrategyByStrategyId(strategyId);//数据库返回的是po
            //放到缓存
            strategyEntity = BeanUtil.copyProperties(strategy, StrategyEntity.class);
            redisService.setValue(key,strategyEntity);
        }
        return strategyEntity;

    }

    @Override
    //mysql
    public StrategyRuleEntity queryStrategyRuleByStrategyIdAndWeight(Long strategyId,String ruleModel) {
        //从strategy_rule中查这个规则
        //封装查询参数
        StrategyRule strategyRuleQuery=new StrategyRule();
        strategyRuleQuery.setStrategyId(strategyId);
        strategyRuleQuery.setRuleModel(ruleModel);
        //数据库查
        StrategyRule strategyRule =  strategyRuleDao.queryStrategyRuleByStrategyIdAndWeight(strategyRuleQuery);
        //转换类型
        StrategyRuleEntity strategyRuleEntity = BeanUtil.copyProperties(strategyRule, StrategyRuleEntity.class);
        return strategyRuleEntity;

    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel) {
        StrategyRule strategyRule = new StrategyRule();
        strategyRule.setStrategyId(strategyId);
        strategyRule.setAwardId(awardId);
        strategyRule.setRuleModel(ruleModel);
        return strategyRuleDao.queryStrategyRuleValue(strategyRule);
    }

    @Override
    //redis    //全量抽奖，不是分段
    public int getRateRange(Long strategyId) {

        return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+strategyId.toString());

    }
    @Override
    //redis
    //分段，获取长度
    public int getRateRange(String key) {
        String cacheKey = Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key;
        if (!redisService.isExists(cacheKey)) {
            throw new AppException(UN_ASSEMBLED_STRATEGY_ARMORY.getCode(), cacheKey + ":" + UN_ASSEMBLED_STRATEGY_ARMORY.getInfo());
        }
        return redisService.getValue(cacheKey);
        //return redisService.getValue(Constants.RedisKey.STRATEGY_RATE_RANGE_KEY+key.toString());

    }

    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId) {
        StrategyAward strategyAward = new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        String ruleModels = strategyAwardDao.queryStrategyAwardRuleModels(strategyAward);
        return StrategyAwardRuleModelVO.builder().ruleModels(ruleModels).build();
    }

    @Override
    public String queryStrategyRuleValueByStrategyIdAndRuleModel(Long strategyId, String s) {
        StrategyRule strategyRule = new StrategyRule();
        strategyRule.setStrategyId(strategyId);
        strategyRule.setRuleModel(s);
        return strategyRuleDao.queryStrategyRuleValueByStrategyIdAndRuleModel(strategyRule);

    }

    @Override
    public RuleTreeVO queryRuleTreeVOByTreeId(String treeId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.RULE_TREE_VO_KEY + treeId;
        RuleTreeVO ruleTreeVOCache = redisService.getValue(cacheKey);
        if (null != ruleTreeVOCache) return ruleTreeVOCache;

        // 从数据库获取
        RuleTree ruleTree = ruleTreeDao.queryRuleTreeByTreeId(treeId);
        List<RuleTreeNode> ruleTreeNodes = ruleTreeNodeDao.queryRuleTreeNodeListByTreeId(treeId);
        List<RuleTreeNodeLine> ruleTreeNodeLines = ruleTreeNodeLineDao.queryRuleTreeNodeLineListByTreeId(treeId);

        // 1. tree node line 转换Map结构
        Map<String, List<RuleTreeNodeLineVO>> ruleTreeNodeLineMap = new HashMap<>();
        for (RuleTreeNodeLine ruleTreeNodeLine : ruleTreeNodeLines) {
            RuleTreeNodeLineVO ruleTreeNodeLineVO = RuleTreeNodeLineVO.builder()
                    .treeId(ruleTreeNodeLine.getTreeId())
                    .ruleNodeFrom(ruleTreeNodeLine.getRuleNodeFrom())
                    .ruleNodeTo(ruleTreeNodeLine.getRuleNodeTo())
                    .ruleLimitType(RuleLimitTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitType()))
                    .ruleLimitValue(RuleLogicCheckTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitValue()))
                    .build();

            List<RuleTreeNodeLineVO> ruleTreeNodeLineVOList = ruleTreeNodeLineMap.computeIfAbsent(ruleTreeNodeLine.getRuleNodeFrom(), k -> new ArrayList<>());
            ruleTreeNodeLineVOList.add(ruleTreeNodeLineVO);
        }

        // 2. tree node 转换为Map结构
        Map<String, RuleTreeNodeVO> treeNodeMap = new HashMap<>();
        for (RuleTreeNode ruleTreeNode : ruleTreeNodes) {
            RuleTreeNodeVO ruleTreeNodeVO = RuleTreeNodeVO.builder()
                    .treeId(ruleTreeNode.getTreeId())
                    .ruleKey(ruleTreeNode.getRuleKey())
                    .ruleDesc(ruleTreeNode.getRuleDesc())
                    .ruleValue(ruleTreeNode.getRuleValue())
                    .treeNodeLineVOList(ruleTreeNodeLineMap.get(ruleTreeNode.getRuleKey()))
                    .build();
            treeNodeMap.put(ruleTreeNode.getRuleKey(), ruleTreeNodeVO);
        }

        // 3. 构建 Rule Tree
        RuleTreeVO ruleTreeVODB = RuleTreeVO.builder()
                .treeId(ruleTree.getTreeId())
                .treeName(ruleTree.getTreeName())
                .treeDesc(ruleTree.getTreeDesc())
                .treeRootRuleNode(ruleTree.getTreeRootRuleKey())
                .treeNodeMap(treeNodeMap)
                .build();
        //树也存在redis了
        redisService.setValue(cacheKey, ruleTreeVODB);
        return ruleTreeVODB;
    }

    @Override
    public void cacheStrategyAwardCount(String cacheKey, Integer awardCount) {
        if (redissonClient.getBucket(cacheKey).isExists()) return;
        redissonClient.getAtomicLong(cacheKey).set(awardCount);
    }

    @Override
    //扣减库存
    public Boolean subtractionAwardStock(String cacheKey) {
        long remain = redissonClient.getAtomicLong(cacheKey).decrementAndGet();
        //判断扣减后的剩余值
        if(remain<0){
            redissonClient.getAtomicLong(cacheKey).set(0);
            return Boolean.FALSE;
        }
        // 1. 按照cacheKey decr 后的值，如 99、98、97 和 key 组成为库存锁的key进行使用。
        // 2. 加锁为了兜底，如果后续有恢复库存，手动处理等，也不会超卖。因为所有的可用库存key，都被加锁了。
        //TODO 每次减了库存都会存一个redis,是不是太多了,好像没有多，因为一旦不小心修改了库存，就会走到这里，然后加锁失败
        String lockKey = cacheKey + "_" + remain;
        boolean lock = redissonClient.getBucket(lockKey).setIfAbsent(0);
        // Boolean lock = redisService.setNx(lockKey);
        if (!lock) {
            log.info("策略奖品库存加锁失败 {}", lockKey);
        }
        return lock;


    }

    @Override
    public void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO) {
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;
        //固定搭配哈哈
        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redissonClient.getBlockingQueue(cacheKey);
        RDelayedQueue<StrategyAwardStockKeyVO> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(strategyAwardStockKeyVO, 3, TimeUnit.SECONDS);

    }

    @Override
    //从延迟队列拿
    public StrategyAwardStockKeyVO takeQueueValue() {
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUEUE_KEY;
        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redissonClient.getBlockingQueue(cacheKey);
        //blockingQueue.take()阻塞方法,我们使用定时任务轮询，那么获取不到不需要阻塞直接返回空就行
        return blockingQueue.poll();
    }

    @Override
    //更新数据库
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        StrategyAward strategyAward = new StrategyAward();
        strategyAward.setStrategyId(strategyId);
        strategyAward.setAwardId(awardId);
        strategyAwardDao.updateStrategyAwardStock(strategyAward);
    }

    @Override
    public StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId + "_" + awardId;
        StrategyAwardEntity strategyAwardEntity = redisService.getValue(cacheKey);
        if (null != strategyAwardEntity) return strategyAwardEntity;
        // 查询数据
        StrategyAward strategyAwardReq = new StrategyAward();
        strategyAwardReq.setStrategyId(strategyId);
        strategyAwardReq.setAwardId(awardId);
        StrategyAward strategyAwardRes = strategyAwardDao.queryStrategyAward(strategyAwardReq);
        // 转换数据
        strategyAwardEntity = StrategyAwardEntity.builder()
                .strategyId(strategyAwardRes.getStrategyId())
                .awardId(strategyAwardRes.getAwardId())
                .awardTitle(strategyAwardRes.getAwardTitle())
                .awardSubtitle(strategyAwardRes.getAwardSubtitle())
                .awardCount(strategyAwardRes.getAwardCount())
                .awardCountSurplus(strategyAwardRes.getAwardCountSurplus())
                .awardRate(strategyAwardRes.getAwardRate())
                .sort(strategyAwardRes.getSort())
                .build();
        // 缓存结果
        redisService.setValue(cacheKey, strategyAwardEntity);
        // 返回数据
        return strategyAwardEntity;

    }

    @Override
    public Long queryStrategyIdByActivityId(Long activityId) {
        return raffleActivityDao.queryStrategyIdByActivityId(activityId);
    }


}
