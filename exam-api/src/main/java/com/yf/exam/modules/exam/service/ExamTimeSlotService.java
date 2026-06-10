package com.yf.exam.modules.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yf.exam.modules.exam.dto.ExamTimeSlotDTO;
import com.yf.exam.modules.exam.entity.ExamTimeSlot;

import java.util.List;

/**
 * <p>
 * 考试时间段业务接口
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
public interface ExamTimeSlotService extends IService<ExamTimeSlot> {

    /**
     * 全量保存某考试的时间段（含段-部门），先删后插
     * @param examId 考试ID
     * @param slots 时间段列表
     */
    void saveAll(String examId, List<ExamTimeSlotDTO> slots);

    /**
     * 查找某考试的时间段列表（含指定/免约部门）
     * @param examId 考试ID
     * @return 时间段列表
     */
    List<ExamTimeSlotDTO> listByExam(String examId);
}
