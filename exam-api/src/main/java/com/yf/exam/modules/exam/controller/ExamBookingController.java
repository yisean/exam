package com.yf.exam.modules.exam.controller;

import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import com.yf.exam.modules.exam.dto.ext.ExamBookingExtDTO;
import com.yf.exam.modules.exam.dto.request.BookingReqDTO;
import com.yf.exam.modules.exam.dto.response.BookingExamRespDTO;
import com.yf.exam.modules.exam.service.ExamBookingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
* <p>
* 考试预约控制器
* </p>
*
* @author plan-001
* @since 2026-05-29
*/
@Api(tags={"考试预约"})
@RestController
@RequestMapping("/exam/api/exam/booking")
public class ExamBookingController extends BaseController {

    @Autowired
    private ExamBookingService baseService;

    /**
     * 列出当前助理可预约的考试及时间段状态
     */
    @RequiresRoles("assistant")
    @ApiOperation(value = "可预约考试列表")
    @RequestMapping(value = "/list-exams", method = { RequestMethod.POST})
    public ApiRest<List<BookingExamRespDTO>> listExams() {
        return super.success(baseService.listBookableExams());
    }

    /**
     * 预约某时间段
     */
    @RequiresRoles("assistant")
    @ApiOperation(value = "预约时间段")
    @RequestMapping(value = "/book", method = { RequestMethod.POST})
    public ApiRest book(@RequestBody BookingReqDTO reqDTO) {
        baseService.book(reqDTO.getSlotId());
        return super.success();
    }

    /**
     * 取消本部门在某考试的预约
     */
    @RequiresRoles("assistant")
    @ApiOperation(value = "取消预约")
    @RequestMapping(value = "/cancel", method = { RequestMethod.POST})
    public ApiRest cancel(@RequestBody BookingReqDTO reqDTO) {
        baseService.cancel(reqDTO.getExamId());
        return super.success();
    }

    /**
     * 改约到同一考试的另一未满时间段
     */
    @RequiresRoles("assistant")
    @ApiOperation(value = "改约时间段")
    @RequestMapping(value = "/change", method = { RequestMethod.POST})
    public ApiRest change(@RequestBody BookingReqDTO reqDTO) {
        baseService.change(reqDTO.getSlotId());
        return super.success();
    }

    /**
     * 管理端：查看某时间段的预约情况（R11b）
     */
    @RequiresRoles(value = {"sa", "teacher"}, logical = Logical.OR)
    @ApiOperation(value = "时间段预约情况")
    @RequestMapping(value = "/slot-bookings", method = { RequestMethod.POST})
    public ApiRest<List<ExamBookingExtDTO>> slotBookings(@RequestBody BookingReqDTO reqDTO) {
        return super.success(baseService.listSlotBookings(reqDTO.getSlotId()));
    }

    /**
     * 管理端：取消某条预约（不受时段开始前限制）
     */
    @RequiresRoles(value = {"sa", "teacher"}, logical = Logical.OR)
    @ApiOperation(value = "管理端取消预约")
    @RequestMapping(value = "/admin-cancel", method = { RequestMethod.POST})
    public ApiRest adminCancel(@RequestBody BookingReqDTO reqDTO) {
        baseService.adminCancel(reqDTO.getBookingId());
        return super.success();
    }
}
