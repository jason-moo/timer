package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("timer_task")
public class TimerTask {


    @TableId
    private String id;

    /**
     * 执行时间
     */
    private Long executeTime;

    /**
     * 参数
     */
    private String customData;

}
