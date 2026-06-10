package com.yf.exam.modules.exam.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 考试时间段DTO
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Data
@ApiModel(value="考试时间段", description="考试时间段")
public class ExamTimeSlotDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    private String id;

    @ApiModelProperty(value = "考试ID")
    private String examId;

    @ApiModelProperty(value = "开放类型：1不限人员 2指定部门 3预约考试", required=true)
    private Integer openType;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "可预约部门数上限（仅预约型有效）")
    private Integer maxDepart;

    @ApiModelProperty(value = "排序")
    private Integer sort;

    @ApiModelProperty(value = "指定部门ID列表（指定部门型）")
    private List<String> departIds;

    @ApiModelProperty(value = "免约部门ID列表（预约型）")
    private List<String> freeDepartIds;
}
