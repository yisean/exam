package com.yf.exam.modules.exam.service;

import com.yf.exam.modules.exam.dto.ExamTimeSlotDTO;
import com.yf.exam.modules.exam.entity.Exam;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 应考资格判定（时间段 + 部门子树 + 时间窗口）
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
public interface ExamEligibilityService {

    /**
     * 学员是否可在列表中看到该考试（含提前可见时长窗口）
     * @param exam 考试
     * @param departId 学员部门
     * @param now 当前时间
     * @return true 可见
     */
    boolean isVisible(Exam exam, String departId, Date now);

    /**
     * 学员当前是否可作答（窗口为时段起止，不含提前可见）
     * @param exam 考试
     * @param departId 学员部门
     * @param now 当前时间
     * @return true 可作答
     */
    boolean canAnswer(Exam exam, String departId, Date now);

    /**
     * 学员当前可作答的时间段（用于学员端展示），无时间段的考试返回空列表
     * @param examId 考试ID
     * @param departId 学员部门
     * @param now 当前时间
     * @return 命中的可见时间段
     */
    List<ExamTimeSlotDTO> listVisibleSlots(String examId, String departId, Date now);
}
