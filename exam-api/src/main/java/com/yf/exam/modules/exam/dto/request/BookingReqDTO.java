package com.yf.exam.modules.exam.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 预约请求类
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Data
@ApiModel(value="预约请求类", description="预约请求类")
public class BookingReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "考试ID")
    private String examId;

    @ApiModelProperty(value = "时间段ID")
    private String slotId;
}
