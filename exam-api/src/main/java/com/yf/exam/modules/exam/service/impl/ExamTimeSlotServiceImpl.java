package com.yf.exam.modules.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.core.exception.ServiceException;
import com.yf.exam.core.utils.BeanMapper;
import com.yf.exam.modules.exam.dto.ExamTimeSlotDTO;
import com.yf.exam.modules.exam.entity.ExamSlotDepart;
import com.yf.exam.modules.exam.entity.ExamTimeSlot;
import com.yf.exam.modules.exam.enums.SlotDepartType;
import com.yf.exam.modules.exam.enums.SlotOpenType;
import com.yf.exam.modules.exam.mapper.ExamSlotDepartMapper;
import com.yf.exam.modules.exam.mapper.ExamTimeSlotMapper;
import com.yf.exam.modules.exam.service.ExamTimeSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 考试时间段业务实现类
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
@Service
public class ExamTimeSlotServiceImpl extends ServiceImpl<ExamTimeSlotMapper, ExamTimeSlot> implements ExamTimeSlotService {

    @Autowired
    private ExamSlotDepartMapper slotDepartMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveAll(String examId, List<ExamTimeSlotDTO> slots) {

        // 先清除旧的时间段及其段-部门
        List<ExamTimeSlot> olds = this.list(new QueryWrapper<ExamTimeSlot>()
                .eq("exam_id", examId));
        if (!CollectionUtils.isEmpty(olds)) {
            List<String> oldSlotIds = olds.stream().map(ExamTimeSlot::getId).collect(Collectors.toList());
            slotDepartMapper.delete(new QueryWrapper<ExamSlotDepart>().in("slot_id", oldSlotIds));
        }
        this.remove(new QueryWrapper<ExamTimeSlot>().eq("exam_id", examId));

        // 无时间段则结束（兼容不使用时间段的考试）
        if (CollectionUtils.isEmpty(slots)) {
            return;
        }

        int sort = 0;
        for (ExamTimeSlotDTO dto : slots) {

            // 校验
            this.validate(dto);

            ExamTimeSlot slot = new ExamTimeSlot();
            BeanMapper.copy(dto, slot);
            String slotId = IdWorker.getIdStr();
            slot.setId(slotId);
            slot.setExamId(examId);
            slot.setSort(sort++);
            this.save(slot);

            // 指定部门（可考）
            if (SlotOpenType.DEPT.equals(dto.getOpenType())) {
                this.saveDeparts(slotId, dto.getDepartIds(), SlotDepartType.ALLOW);
            }

            // 预约型的免约部门（可选）
            if (SlotOpenType.BOOKING.equals(dto.getOpenType())) {
                this.saveDeparts(slotId, dto.getFreeDepartIds(), SlotDepartType.FREE);
            }
        }
    }

    @Override
    public List<ExamTimeSlotDTO> listByExam(String examId) {

        List<ExamTimeSlot> slots = this.list(new QueryWrapper<ExamTimeSlot>()
                .eq("exam_id", examId)
                .orderByAsc("sort"));

        List<ExamTimeSlotDTO> list = new ArrayList<>();
        for (ExamTimeSlot slot : slots) {
            ExamTimeSlotDTO dto = new ExamTimeSlotDTO();
            BeanMapper.copy(slot, dto);
            dto.setDepartIds(this.listDepartIds(slot.getId(), SlotDepartType.ALLOW));
            dto.setFreeDepartIds(this.listDepartIds(slot.getId(), SlotDepartType.FREE));
            list.add(dto);
        }
        return list;
    }

    /**
     * 时间段配置校验
     */
    private void validate(ExamTimeSlotDTO dto) {
        if (dto.getOpenType() == null) {
            throw new ServiceException(1, "时间段开放类型不能为空！");
        }
        if (SlotOpenType.BOOKING.equals(dto.getOpenType())
                && (dto.getMaxDepart() == null || dto.getMaxDepart() <= 0)) {
            throw new ServiceException(1, "预约型时间段必须设置可预约部门数上限！");
        }
        if (SlotOpenType.DEPT.equals(dto.getOpenType())
                && CollectionUtils.isEmpty(dto.getDepartIds())) {
            throw new ServiceException(1, "指定部门型时间段必须至少选择一个部门！");
        }
    }

    /**
     * 保存某时间段的部门
     */
    private void saveDeparts(String slotId, List<String> departIds, Integer departType) {
        if (CollectionUtils.isEmpty(departIds)) {
            return;
        }
        for (String departId : departIds) {
            ExamSlotDepart sd = new ExamSlotDepart();
            sd.setId(IdWorker.getIdStr());
            sd.setSlotId(slotId);
            sd.setDepartId(departId);
            sd.setDepartType(departType);
            slotDepartMapper.insert(sd);
        }
    }

    /**
     * 列出某时间段某类型的部门ID
     */
    private List<String> listDepartIds(String slotId, Integer departType) {
        List<ExamSlotDepart> list = slotDepartMapper.selectList(new QueryWrapper<ExamSlotDepart>()
                .eq("slot_id", slotId)
                .eq("depart_type", departType));
        return list.stream().map(ExamSlotDepart::getDepartId).collect(Collectors.toList());
    }
}
