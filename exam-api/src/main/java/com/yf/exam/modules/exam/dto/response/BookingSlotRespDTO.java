package com.yf.exam.modules.exam.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 助理预约-时间段响应类
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Data
@ApiModel(value="助理预约时间段", description="助理预约时间段")
public class BookingSlotRespDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "时间段ID")
    private String id;

    @ApiModelProperty(value = "考试ID")
    private String examId;

    @ApiModelProperty(value = "开放类型")
    private Integer openType;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "可预约部门数上限")
    private Integer maxDepart;

    @ApiModelProperty(value = "已预约部门数")
    private Integer bookedCount;

    @ApiModelProperty(value = "对当前部门的状态：1可约 2已满 3已开始 4本部门已约")
    private Integer status;
}
