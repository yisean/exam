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
* 考试时间段实体类
* </p>
*
* @author plan-001
* @since 2026-05-29
*/
@Data
@TableName("el_exam_time_slot")
public class ExamTimeSlot extends Model<ExamTimeSlot> {

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
    * 开放类型：1不限人员 2指定部门 3预约考试
    */
    @TableField("open_type")
    private Integer openType;

    /**
    * 开始时间
    */
    @TableField("start_time")
    private Date startTime;

    /**
    * 结束时间
    */
    @TableField("end_time")
    private Date endTime;

    /**
    * 可预约部门数上限（仅预约型有效）
    */
    @TableField("max_depart")
    private Integer maxDepart;

    /**
    * 排序
    */
    @TableField("sort")
    private Integer sort;

}
