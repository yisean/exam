package com.yf.exam.modules.exam.enums;

/**
 * <p>
 * 考试时间段开放类型
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
public interface SlotOpenType {

    /**
     * 不限人员
     */
    Integer OPEN = 1;

    /**
     * 指定部门
     */
    Integer DEPT = 2;

    /**
     * 预约考试
     */
    Integer BOOKING = 3;
}
