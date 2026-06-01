package com.yf.exam.modules.exam.dto.ext;

import com.yf.exam.modules.exam.dto.ExamBookingDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 考试预约记录扩展响应类（含部门名、预约人姓名）
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Data
@ApiModel(value="考试预约记录扩展", description="考试预约记录扩展")
public class ExamBookingExtDTO extends ExamBookingDTO {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "部门名称")
    private String departName;

    @ApiModelProperty(value = "预约人姓名")
    private String userName;

    @ApiModelProperty(value = "该部门(含下级)在本场考试是否已有试卷(用于取消前二次确认)")
    private Boolean hasPaper;
}
