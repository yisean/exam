package com.yf.exam.modules.exam.enums;

/**
 * <p>
 * 时间段-部门类型
 * </p>
 *
 * @author plan-001
 * @since 2026-05-29
 */
public interface SlotDepartType {

    /**
     * 指定部门（可考）
     */
    Integer ALLOW = 1;

    /**
     * 预约型免约部门（无需预约即可考）
     */
    Integer FREE = 2;
}
