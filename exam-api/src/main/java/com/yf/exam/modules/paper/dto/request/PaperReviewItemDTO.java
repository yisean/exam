package com.yf.exam.modules.paper.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 阅卷·单题评分项
 * </p>
 *
 * @author yf
 * @since 2026-06-14
 */
@Data
@ApiModel(value = "阅卷单题评分项", description = "阅卷单题评分项")
public class PaperReviewItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "题目ID", required = true)
    private String quId;

    @ApiModelProperty(value = "本题得分（0~该题满分的整数）", required = true)
    private Integer score;

    @ApiModelProperty(value = "点评（选填，考生可见）")
    private String comment;

}
