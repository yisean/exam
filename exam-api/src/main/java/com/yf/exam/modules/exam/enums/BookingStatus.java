package com.yf.exam.modules.exam.enums;

/**
 * <p>
 * 预约型时间段对某部门的状态
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
public interface BookingStatus {

    /**
     * 可预约
     */
    Integer BOOKABLE = 1;

    /**
     * 已约满
     */
    Integer FULL = 2;

    /**
     * 已开始（已截止预约）
     */
    Integer STARTED = 3;

    /**
     * 本部门已预约
     */
    Integer MINE = 4;
}
