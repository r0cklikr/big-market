package cn.bugstack.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public  class   MqMessage<T> {
    private String id;
    private Date timestamp;
    private T data;





}
