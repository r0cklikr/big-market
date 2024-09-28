package cn.bugstack.domain.award.event;

import cn.bugstack.types.MqMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 用户奖品记录事件消息
 * @create 2024-04-06 09:43
 */
@Component
public class SendAwardMessageEvent {

  //  @Value("${spring.rabbitmq.topic.send_award}")
    private String topic = "send.award";


    public MqMessage buildEventMessage(SendAwardMessage data) {
        MqMessage mqMessage = new MqMessage();
        mqMessage.setData(data);
        mqMessage.setId(RandomStringUtils.randomNumeric(11));
        mqMessage.setTimestamp(new Date());

        return mqMessage;
    }

    public String topic() {
        return topic;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendAwardMessage {
        /**
         * 用户ID
         */
        private String userId;
        /**
         * 奖品ID
         */
        private Integer awardId;
        /**
         * 奖品标题（名称）
         */
        private String awardTitle;
    }

}
