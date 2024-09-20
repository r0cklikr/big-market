package cn.bugstack.domain.strategy.service.rule.tree.factory.engine.impl;

import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeLineVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeNodeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.service.rule.tree.ILogicTreeNode;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 决策树引擎
 * @create 2024-01-27 11:34
 */
@Slf4j
public class DecisionTreeEngine implements IDecisionTreeEngine {

    private final Map<String, ILogicTreeNode> logicTreeNodeGroup;

    private final RuleTreeVO ruleTreeVO;

    public DecisionTreeEngine(Map<String, ILogicTreeNode> logicTreeNodeGroup, RuleTreeVO ruleTreeVO) {
        this.logicTreeNodeGroup = logicTreeNodeGroup;
        this.ruleTreeVO = ruleTreeVO;//树的信息，根节点，通过节点名字获得value是节点的map
    }

    @Override
    //每个节点都有两个子节点，一个是放行一个是接管，那么当本节点经过处理后，有一个结果，如果这个结果是放行那么就走放行的节点，否则走接管节点
    //每个节点都有一个返回值
    public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId) {
        DefaultTreeFactory.StrategyAwardVO strategyAwardData = null;

        // 获取根节点
        String nextNode = ruleTreeVO.getTreeRootRuleNode();
        Map<String, RuleTreeNodeVO> treeNodeMap = ruleTreeVO.getTreeNodeMap();//拿到节点名与节点的映射

        // 获取起始节点「根节点记录了第一个要执行的规则」
        RuleTreeNodeVO ruleTreeNode = treeNodeMap.get(nextNode);//根据根节点的名字获取到根节点

        while (null != nextNode) {
            // 通过rulekey(rule_lock)获取该节点的处理器
            ILogicTreeNode logicTreeNode = logicTreeNodeGroup.get(ruleTreeNode.getRuleKey());
            //TODO,难道不是根据奖品的rule_value来吗？？？
            String ruleValue = ruleTreeNode.getRuleValue();//获取当前节点rule value,因为lock节点需要通过lock数判断，还有幸运奖需要返回一个区间的随机值

            // 调用处理器
            DefaultTreeFactory.TreeActionEntity logicEntity = logicTreeNode.logic(userId, strategyId, awardId,ruleValue);//拦截，放行
            RuleLogicCheckTypeVO ruleLogicCheckTypeVO = logicEntity.getRuleLogicCheckType();
            strategyAwardData = logicEntity.getStrategyAwardVO();
            log.info("决策树引擎【{}】treeId:{} node:{} code:{}", ruleTreeVO.getTreeName(), ruleTreeVO.getTreeId(), nextNode, ruleLogicCheckTypeVO.getCode());

            // 获取下个节点的key                  本节点处理结果             该节点拥有的子节点，以及节点的进入条件(放行，接管)
            nextNode = nextNode(ruleLogicCheckTypeVO.getCode(), ruleTreeNode.getTreeNodeLineVOList());
            //更新当前处理的节点
            ruleTreeNode = treeNodeMap.get(nextNode);
        }

        // 过滤完:返回最终结果
        return strategyAwardData;

    }
    public String nextNode(String matterValue, List<RuleTreeNodeLineVO> treeNodeLineVOList) {
        if (null == treeNodeLineVOList || treeNodeLineVOList.isEmpty()) return null;
        for (RuleTreeNodeLineVO nodeLine : treeNodeLineVOList) {
            if (decisionLogic(matterValue, nodeLine)) {
                return nodeLine.getRuleNodeTo();
            }
        }
        //TODO 没有下一个节点应该直接放行，不需要抛异常
        return null;
       // throw new RuntimeException("决策树引擎，nextNode 计算失败，未找到可执行节点！");
    }

    public boolean decisionLogic(String matterValue, RuleTreeNodeLineVO nodeLine) {
        switch (nodeLine.getRuleLimitType()) {
            case EQUAL:
                return matterValue.equals(nodeLine.getRuleLimitValue().getCode());
            // 以下规则暂时不需要实现
            case GT:
            case LT:
            case GE:
            case LE:
            default:
                return false;
        }
    }

}
