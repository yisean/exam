package com.yf.exam.modules.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yf.exam.core.enums.OpenType;
import com.yf.exam.modules.exam.dto.ExamTimeSlotDTO;
import com.yf.exam.modules.exam.entity.Exam;
import com.yf.exam.modules.exam.entity.ExamBooking;
import com.yf.exam.modules.exam.enums.SlotOpenType;
import com.yf.exam.modules.exam.mapper.ExamBookingMapper;
import com.yf.exam.modules.exam.service.ExamDepartService;
import com.yf.exam.modules.exam.service.ExamEligibilityService;
import com.yf.exam.modules.exam.service.ExamTimeSlotService;
import com.yf.exam.modules.sys.config.dto.SysConfigDTO;
import com.yf.exam.modules.sys.config.service.SysConfigService;
import com.yf.exam.modules.sys.depart.service.SysDepartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 应考资格判定实现类
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Service
public class ExamEligibilityServiceImpl implements ExamEligibilityService {

    @Autowired
    private ExamTimeSlotService examTimeSlotService;

    @Autowired
    private ExamDepartService examDepartService;

    @Autowired
    private SysDepartService sysDepartService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ExamBookingMapper examBookingMapper;

    @Override
    public boolean isVisible(Exam exam, String departId, Date now) {
        List<ExamTimeSlotDTO> slots = examTimeSlotService.listByExam(exam.getId());
        if (CollectionUtils.isEmpty(slots)) {
            return legacyVisible(exam, departId);
        }
        int advance = advanceMinutes();
        for (ExamTimeSlotDTO slot : slots) {
            if (eligibleForSlot(slot, departId) && inVisibleWindow(slot, now, advance)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canAnswer(Exam exam, String departId, Date now) {
        List<ExamTimeSlotDTO> slots = examTimeSlotService.listByExam(exam.getId());
        if (CollectionUtils.isEmpty(slots)) {
            // 无时间段：沿用历史部门可见规则（时间/状态由调用方既有逻辑把关）
            return legacyVisible(exam, departId);
        }
        for (ExamTimeSlotDTO slot : slots) {
            if (eligibleForSlot(slot, departId) && inAnswerWindow(slot, now)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ExamTimeSlotDTO> listVisibleSlots(String examId, String departId, Date now) {
        List<ExamTimeSlotDTO> slots = examTimeSlotService.listByExam(examId);
        if (CollectionUtils.isEmpty(slots)) {
            return new ArrayList<>();
        }
        int advance = advanceMinutes();
        return slots.stream()
                .filter(s -> eligibleForSlot(s, departId) && inVisibleWindow(s, now, advance))
                .collect(Collectors.toList());
    }

    // ----------------- 私有辅助 -----------------

    /**
     * 历史（无时间段）考试的部门可见规则，保持与旧 online 查询一致：
     * 公开/定员对所有人可见；部门型需学员部门在 el_exam_depart（直配，不含下级）。
     */
    private boolean legacyVisible(Exam exam, String departId) {
        Integer openType = exam.getOpenType();
        if (OpenType.DEPT_OPEN.equals(openType)) {
            if (!StringUtils.hasText(departId)) {
                return false;
            }
            List<String> departIds = examDepartService.listByExam(exam.getId());
            return departIds != null && departIds.contains(departId);
        }
        // 公开(1) 与 定员(3) 维持旧逻辑：列表可见
        return true;
    }

    /**
     * 学员部门是否对该时间段有资格（不含时间窗口判断）
     */
    private boolean eligibleForSlot(ExamTimeSlotDTO slot, String departId) {
        if (SlotOpenType.OPEN.equals(slot.getOpenType())) {
            return true;
        }
        if (!StringUtils.hasText(departId)) {
            return false;
        }
        if (SlotOpenType.DEPT.equals(slot.getOpenType())) {
            return departInSubtreeOfAny(departId, slot.getDepartIds());
        }
        if (SlotOpenType.BOOKING.equals(slot.getOpenType())) {
            // 已预约部门 ∪ 免约部门
            Set<String> roots = new HashSet<>();
            List<ExamBooking> bookings = examBookingMapper.selectList(
                    new QueryWrapper<ExamBooking>().eq("slot_id", slot.getId()));
            for (ExamBooking b : bookings) {
                roots.add(b.getDepartId());
            }
            if (!CollectionUtils.isEmpty(slot.getFreeDepartIds())) {
                roots.addAll(slot.getFreeDepartIds());
            }
            return departInSubtreeOfAny(departId, new ArrayList<>(roots));
        }
        return false;
    }

    /**
     * departId 是否落在任一 root 部门的子树（含 root 自身）内
     */
    private boolean departInSubtreeOfAny(String departId, List<String> roots) {
        if (CollectionUtils.isEmpty(roots)) {
            return false;
        }
        for (String root : roots) {
            if (departId.equals(root)) {
                return true;
            }
            List<String> subtree = sysDepartService.listAllSubIds(root);
            if (subtree != null && subtree.contains(departId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 可见窗口：[start - advance, end]
     */
    private boolean inVisibleWindow(ExamTimeSlotDTO slot, Date now, int advanceMinutes) {
        Date start = slot.getStartTime();
        Date end = slot.getEndTime();
        if (start != null) {
            long visibleFrom = start.getTime() - (long) advanceMinutes * 60_000L;
            if (now.getTime() < visibleFrom) {
                return false;
            }
        }
        return end == null || !now.after(end);
    }

    /**
     * 可作答窗口：[start, end]
     */
    private boolean inAnswerWindow(ExamTimeSlotDTO slot, Date now) {
        Date start = slot.getStartTime();
        Date end = slot.getEndTime();
        if (start != null && now.before(start)) {
            return false;
        }
        return end == null || !now.after(end);
    }

    private int advanceMinutes() {
        SysConfigDTO config = sysConfigService.find();
        if (config != null && config.getAdvanceVisibleMinutes() != null) {
            return Math.max(0, config.getAdvanceVisibleMinutes());
        }
        return 0;
    }
}
