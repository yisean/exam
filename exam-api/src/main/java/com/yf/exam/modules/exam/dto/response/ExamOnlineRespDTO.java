package com.yf.exam.modules.exam.dto.response;

import com.yf.exam.modules.exam.dto.ExamDTO;
import com.yf.exam.modules.exam.dto.ExamTimeSlotDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
* <p>
* 考试分页响应类
* </p>
*
* @author 聪明笨狗
* @since 2020-07-25 16:18
*/
@Data
@ApiModel(value="在线考试分页响应类", description="在线考试分页响应类")
public class ExamOnlineRespDTO extends ExamDTO {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "对该学员可见的时间段（含窗口/类型）")
    private List<ExamTimeSlotDTO> timeSlots;

    @ApiModelProperty(value = "当前是否可作答")
    private Boolean canAnswer;

}
