package com.yf.exam.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;

/**
* <p>
* 考试预约记录实体类
* </p>
*
* @author plan-001
* @since 2026-05-29
*/
@Data
@TableName("el_exam_booking")
public class ExamBooking extends Model<ExamBooking> {

    private static final long serialVersionUID = 1L;

    /**
    * ID
    */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
    * 考试ID
    */
    @TableField("exam_id")
    private String examId;

    /**
    * 时间段ID
    */
    @TableField("slot_id")
    private String slotId;

    /**
    * 预约的部门ID
    */
    @TableField("depart_id")
    private String departId;

    /**
    * 预约人（考试助理）
    */
    @TableField("user_id")
    private String userId;

    /**
    * 预约时间
    */
    @TableField("create_time")
    private Date createTime;

}
