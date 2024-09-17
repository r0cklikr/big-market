package cn.bugstack.domain.strategy.model.entity;

import cn.bugstack.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrategyRuleEntity {
    /** 抽奖策略ID */
    private Long strategyId;
    /** 抽奖奖品ID【规则类型为策略，则不需要奖品ID】 */
    private Integer awardId;
    /** 抽象规则类型；1-策略规则、2-奖品规则 */
    private Integer ruleType;
    /** 抽奖规则类型【rule_random - 随机值计算、rule_lock - 抽奖几次后解锁、rule_luck_award - 幸运奖(兜底奖品)】 */
    private String ruleModel;
    /** 抽奖规则比值 */
    private String ruleValue;
    /** 抽奖规则描述 */
    private String ruleDesc;

    public Map<String, List<Integer>> getRuleWeightValues(){
        Map<String, List<Integer>> map=new HashMap<>();
        String[] split = ruleValue.split(" ");

        for(String s:split){
            String[] value=s.split(":");
            if(value.length!=2){
                throw new IllegalArgumentException("weight的value参数异常");
            }
            String arg = value[1];
            String[] awardIds = arg.split(",");
            List<Integer> list=Arrays.stream(awardIds).map(Integer::valueOf).collect(Collectors.toList());
            map.put(s,list);
        }
        return map;

    }

}
