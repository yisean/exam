package com.yf.exam.modules.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

/**
* <p>
* 考试时间段部门实体类
* </p>
*
* @author plan-001
* @since 2026-05-29
*/
@Data
@TableName("el_exam_slot_depart")
public class ExamSlotDepart extends Model<ExamSlotDepart> {

    private static final long serialVersionUID = 1L;

    /**
    * ID
    */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
    * 时间段ID
    */
    @TableField("slot_id")
    private String slotId;

    /**
    * 部门ID
    */
    @TableField("depart_id")
    private String departId;

    /**
    * 部门类型：1指定部门(可考) 2预约型免约部门
    */
    @TableField("depart_type")
    private Integer departType;

}
