package com.yf.exam.modules.paper.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 整份试卷阅卷请求
 * </p>
 *
 * @author yf
 * @since 2026-06-14
 */
@Data
@ApiModel(value = "整份试卷阅卷请求", description = "整份试卷阅卷请求")
public class PaperReviewReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "试卷ID", required = true)
    private String paperId;

    @ApiModelProperty(value = "各简答题评分项", required = true)
    private List<PaperReviewItemDTO> items;

}
