import { post } from '@/utils/request'

/**
 * 当前助理可预约的考试及时间段
 */
export function fetchBookingExams() {
  return post('/exam/api/exam/booking/list-exams', {})
}

/**
 * 预约某时间段
 * @param slotId
 */
export function bookSlot(slotId) {
  return post('/exam/api/exam/booking/book', { slotId: slotId })
}

/**
 * 取消本部门在某考试的预约
 * @param examId
 */
export function cancelBooking(examId) {
  return post('/exam/api/exam/booking/cancel', { examId: examId })
}

/**
 * 改约到另一时间段
 * @param slotId
 */
export function changeBooking(slotId) {
  return post('/exam/api/exam/booking/change', { slotId: slotId })
}

/**
 * 管理端：查看某时间段的预约情况
 * @param slotId
 */
export function fetchSlotBookings(slotId) {
  return post('/exam/api/exam/booking/slot-bookings', { slotId: slotId })
}
