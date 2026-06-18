package com.yf.exam.modules.paper.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
* <p>
* 试卷考题请求类
* </p>
*
* @author 聪明笨狗
* @since 2020-05-25 17:31
*/
@Data
@ApiModel(value="试卷考题", description="试卷考题")
public class PaperQuDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    @ApiModelProperty(value = "ID", required=true)
    private String id;

    @ApiModelProperty(value = "试卷ID", required=true)
    private String paperId;

    @ApiModelProperty(value = "题目ID", required=true)
    private String quId;

    @ApiModelProperty(value = "题目类型", required=true)
    private Integer quType;

    @ApiModelProperty(value = "是否已答", required=true)
    private Boolean answered;

    @ApiModelProperty(value = "主观答案", required=true)
    private String answer;

    @ApiModelProperty(value = "问题排序", required=true)
    private Integer sort;

    @ApiModelProperty(value = "单题分分值", required=true)
    private Integer score;

    @ApiModelProperty(value = "实际得分(主观题)", required=true)
    private Integer actualScore;

    @ApiModelProperty(value = "是否答对", required=true)
    private Boolean isRight;

    @ApiModelProperty(value = "父题在本试卷中的ID（综合题子题用，普通题为空）")
    private String parentId;

    @ApiModelProperty(value = "阅卷点评（简答题人工阅卷填写，考生可见）")
    private String comment;

    @ApiModelProperty(value = "子题列表（仅综合题父题：其5个子题）")
    private List<PaperQuDTO> subList;

}
