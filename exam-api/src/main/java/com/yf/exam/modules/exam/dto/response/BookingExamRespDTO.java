package com.yf.exam.modules.exam.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 助理预约-考试响应类
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Data
@ApiModel(value="助理预约考试", description="助理预约考试")
public class BookingExamRespDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "考试ID")
    private String examId;

    @ApiModelProperty(value = "考试名称")
    private String title;

    @ApiModelProperty(value = "考试描述")
    private String content;

    @ApiModelProperty(value = "总时长（分钟）")
    private Integer totalTime;

    @ApiModelProperty(value = "及格分数")
    private Integer qualifyScore;

    @ApiModelProperty(value = "本部门已预约的时间段ID（未预约为空）")
    private String bookedSlotId;

    @ApiModelProperty(value = "预约型时间段列表")
    private List<BookingSlotRespDTO> slots;
}
