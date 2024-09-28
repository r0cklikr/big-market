package cn.bugstack.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 参与抽奖活动实体对象
 * @create 2024-04-04 20:02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartakeRaffleActivityEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 活动ID
     */
    private Long activityId;


}
