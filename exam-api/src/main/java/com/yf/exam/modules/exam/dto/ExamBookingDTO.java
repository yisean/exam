package com.yf.exam.modules.exam.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 考试预约记录DTO
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Data
@ApiModel(value="考试预约记录", description="考试预约记录")
public class ExamBookingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    private String id;

    @ApiModelProperty(value = "考试ID")
    private String examId;

    @ApiModelProperty(value = "时间段ID")
    private String slotId;

    @ApiModelProperty(value = "预约的部门ID")
    private String departId;

    @ApiModelProperty(value = "预约人（考试助理）")
    private String userId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "预约时间")
    private Date createTime;
}
