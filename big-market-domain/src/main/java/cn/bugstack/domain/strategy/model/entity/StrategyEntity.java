package cn.bugstack.domain.strategy.model.entity;

import cn.bugstack.types.common.Constants;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrategyEntity {

    /** 抽奖策略ID */
    private Long strategyId;
    //抽奖规则模型
    private String ruleModels;
    /** 抽奖策略描述 */
    private String strategyDesc;

    public String[] getRuleModels() {
        //可能是空的
        if(StrUtil.isEmpty(ruleModels)){
            return null;
        }
        String[] split = ruleModels.split(Constants.SPLIT);
        return split;

    }

    public String getRuleWeight(){

        String[] s = getRuleModels();
        if(s==null) return null;
        for(String temp: s){
            if("rule_weight".equals(temp)){
                return temp;
            }
        }
        return null;
    }

}
