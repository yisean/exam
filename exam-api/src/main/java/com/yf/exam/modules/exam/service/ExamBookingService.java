package com.yf.exam.modules.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yf.exam.modules.exam.dto.ext.ExamBookingExtDTO;
import com.yf.exam.modules.exam.dto.response.BookingExamRespDTO;
import com.yf.exam.modules.exam.entity.ExamBooking;

import java.util.List;

/**
 * <p>
 * 考试预约业务接口
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
public interface ExamBookingService extends IService<ExamBooking> {

    /**
     * 列出当前助理可预约的考试及其预约型时间段状态
     * @return 考试列表
     */
    List<BookingExamRespDTO> listBookableExams();

    /**
     * 为当前助理所属部门预约某时间段
     * @param slotId 时间段ID
     */
    void book(String slotId);

    /**
     * 取消当前助理所属部门在某考试的预约
     * @param examId 考试ID
     */
    void cancel(String examId);

    /**
     * 改约到同一考试的另一未满预约型时间段
     * @param slotId 目标时间段ID
     */
    void change(String slotId);

    /**
     * 查看某时间段的预约情况（管理端 R11b）
     * @param slotId 时间段ID
     * @return 预约记录（含部门名、预约人姓名）
     */
    List<ExamBookingExtDTO> listSlotBookings(String slotId);
}
