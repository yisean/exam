package com.yf.exam.modules.qu.enums;


/**
 * 题目类型
 * @author bool 
 * @date 2019-10-30 13:11
 */
public interface QuType {

    /**
     * 单选题
     */
    Integer RADIO = 1;

    /**
     * 多选题
     */
    Integer MULTI = 2;

    /**
     * 判断题
     */
    Integer JUDGE = 3;

    /**
     * 不定项（1个或多个正确答案，固定半分制判分）
     */
    Integer UNCERTAIN = 5;

    /**
     * 综合题（父题，含恰好5个子题；子题题型∈{1,2,3,5}，不可嵌套综合题）
     */
    Integer COMPOSITE = 6;

}
