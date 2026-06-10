package com.yf.exam.modules.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yf.exam.modules.exam.dto.ext.ExamBookingExtDTO;
import com.yf.exam.modules.exam.entity.ExamBooking;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 考试预约记录 Mapper
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
public interface ExamBookingMapper extends BaseMapper<ExamBooking> {

    /**
     * 查找某时间段的预约记录（含部门名、预约人姓名）
     * @param slotId
     * @return
     */
    List<ExamBookingExtDTO> listSlotBookings(@Param("slotId") String slotId);
}
