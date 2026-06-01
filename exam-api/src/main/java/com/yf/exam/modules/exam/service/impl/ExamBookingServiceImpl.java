package com.yf.exam.modules.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.core.exception.ServiceException;
import com.yf.exam.core.utils.BeanMapper;
import com.yf.exam.modules.exam.dto.ext.ExamBookingExtDTO;
import com.yf.exam.modules.exam.dto.response.BookingExamRespDTO;
import com.yf.exam.modules.exam.dto.response.BookingSlotRespDTO;
import com.yf.exam.modules.exam.entity.Exam;
import com.yf.exam.modules.exam.entity.ExamBooking;
import com.yf.exam.modules.exam.entity.ExamTimeSlot;
import com.yf.exam.modules.exam.enums.BookingStatus;
import com.yf.exam.modules.exam.enums.SlotOpenType;
import com.yf.exam.modules.exam.mapper.ExamBookingMapper;
import com.yf.exam.modules.exam.service.ExamBookingService;
import com.yf.exam.modules.exam.service.ExamService;
import com.yf.exam.modules.exam.service.ExamTimeSlotService;
import com.yf.exam.modules.user.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 考试预约业务实现类
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Service
public class ExamBookingServiceImpl extends ServiceImpl<ExamBookingMapper, ExamBooking> implements ExamBookingService {

    @Autowired
    private ExamTimeSlotService examTimeSlotService;

    @Autowired
    private ExamService examService;

    @Override
    public List<BookingExamRespDTO> listBookableExams() {

        String departId = UserUtils.getDepartId();

        // 所有预约型时间段
        List<ExamTimeSlot> slots = examTimeSlotService.list(new QueryWrapper<ExamTimeSlot>()
                .eq("open_type", SlotOpenType.BOOKING)
                .orderByAsc("exam_id").orderByAsc("sort"));

        if (CollectionUtils.isEmpty(slots)) {
            return new ArrayList<>();
        }

        // 本部门已有的预约：exam_id -> slot_id
        List<ExamBooking> myBookings = this.list(new QueryWrapper<ExamBooking>()
                .eq("depart_id", departId));
        Map<String, String> bookedByExam = myBookings.stream()
                .collect(Collectors.toMap(ExamBooking::getExamId, ExamBooking::getSlotId, (a, b) -> a));

        Date now = new Date();

        // 按考试聚合
        Map<String, BookingExamRespDTO> examMap = new LinkedHashMap<>();
        for (ExamTimeSlot slot : slots) {

            BookingExamRespDTO exam = examMap.get(slot.getExamId());
            if (exam == null) {
                Exam e = examService.getById(slot.getExamId());
                if (e == null) {
                    continue;
                }
                exam = new BookingExamRespDTO();
                exam.setExamId(e.getId());
                exam.setTitle(e.getTitle());
                exam.setContent(e.getContent());
                exam.setTotalTime(e.getTotalTime());
                exam.setQualifyScore(e.getQualifyScore());
                exam.setBookedSlotId(bookedByExam.get(e.getId()));
                exam.setSlots(new ArrayList<>());
                examMap.put(e.getId(), exam);
            }

            BookingSlotRespDTO sd = new BookingSlotRespDTO();
            BeanMapper.copy(slot, sd);
            int bookedCount = this.countBySlot(slot.getId());
            sd.setBookedCount(bookedCount);
            sd.setStatus(this.calcStatus(slot, bookedCount, slot.getId().equals(exam.getBookedSlotId()), now));
            exam.getSlots().add(sd);
        }

        return new ArrayList<>(examMap.values());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void book(String slotId) {
        String departId = UserUtils.getDepartId();
        String userId = UserUtils.getUserId();

        ExamTimeSlot slot = this.loadBookingSlot(slotId);

        // R9 已开始不可约
        if (this.isStarted(slot, new Date())) {
            throw new ServiceException(1, "该时间段已开始，无法预约！");
        }

        // R7 本部门本场是否已约
        int booked = this.count(new QueryWrapper<ExamBooking>()
                .eq("exam_id", slot.getExamId())
                .eq("depart_id", departId));
        if (booked > 0) {
            throw new ServiceException(1, "本部门在该考试已预约时间段，请勿重复预约！");
        }

        // R8 容量
        if (this.countBySlot(slotId) >= slot.getMaxDepart()) {
            throw new ServiceException(1, "该时间段预约名额已满！");
        }

        this.insertBooking(slot, departId, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(String examId) {
        String departId = UserUtils.getDepartId();

        ExamBooking booking = this.getOne(new QueryWrapper<ExamBooking>()
                .eq("exam_id", examId)
                .eq("depart_id", departId), false);
        if (booking == null) {
            throw new ServiceException(1, "本部门未预约该考试！");
        }

        ExamTimeSlot slot = examTimeSlotService.getById(booking.getSlotId());
        // R10 仅时段开始前可取消
        if (slot != null && this.isStarted(slot, new Date())) {
            throw new ServiceException(1, "时间段已开始，无法取消预约！");
        }

        this.removeById(booking.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void change(String slotId) {
        String departId = UserUtils.getDepartId();
        String userId = UserUtils.getUserId();

        ExamTimeSlot target = this.loadBookingSlot(slotId);
        Date now = new Date();

        // 目标段校验：未开始、未满
        if (this.isStarted(target, now)) {
            throw new ServiceException(1, "目标时间段已开始，无法改约！");
        }
        if (this.countBySlot(slotId) >= target.getMaxDepart()) {
            throw new ServiceException(1, "目标时间段预约名额已满！");
        }

        // 删除本部门在该考试的旧预约（若有）
        ExamBooking old = this.getOne(new QueryWrapper<ExamBooking>()
                .eq("exam_id", target.getExamId())
                .eq("depart_id", departId), false);
        if (old != null) {
            if (old.getSlotId().equals(slotId)) {
                throw new ServiceException(1, "本部门已预约该时间段！");
            }
            this.removeById(old.getId());
        }

        this.insertBooking(target, departId, userId);
    }

    @Override
    public List<ExamBookingExtDTO> listSlotBookings(String slotId) {
        return baseMapper.listSlotBookings(slotId);
    }

    // ----------------- 私有辅助 -----------------

    private ExamTimeSlot loadBookingSlot(String slotId) {
        ExamTimeSlot slot = examTimeSlotService.getById(slotId);
        if (slot == null) {
            throw new ServiceException(1, "时间段不存在！");
        }
        if (!SlotOpenType.BOOKING.equals(slot.getOpenType())) {
            throw new ServiceException(1, "该时间段不支持预约！");
        }
        return slot;
    }

    private void insertBooking(ExamTimeSlot slot, String departId, String userId) {
        ExamBooking booking = new ExamBooking();
        booking.setId(IdWorker.getIdStr());
        booking.setExamId(slot.getExamId());
        booking.setSlotId(slot.getId());
        booking.setDepartId(departId);
        booking.setUserId(userId);
        booking.setCreateTime(new Date());
        try {
            this.save(booking);
        } catch (DuplicateKeyException e) {
            // 唯一键 (exam_id, depart_id) 兜底并发/重复
            throw new ServiceException(1, "本部门在该考试已预约时间段，请勿重复预约！");
        }
    }

    private int countBySlot(String slotId) {
        return this.count(new QueryWrapper<ExamBooking>().eq("slot_id", slotId));
    }

    private boolean isStarted(ExamTimeSlot slot, Date now) {
        return slot.getStartTime() != null && !now.before(slot.getStartTime());
    }

    private Integer calcStatus(ExamTimeSlot slot, int bookedCount, boolean mine, Date now) {
        if (mine) {
            return BookingStatus.MINE;
        }
        if (this.isStarted(slot, now)) {
            return BookingStatus.STARTED;
        }
        if (slot.getMaxDepart() != null && bookedCount >= slot.getMaxDepart()) {
            return BookingStatus.FULL;
        }
        return BookingStatus.BOOKABLE;
    }
}
