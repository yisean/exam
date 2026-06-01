<template>
  <div class="app-container">

    <el-alert
      title="您是本部门考试助理，可代表本部门（含所有下级部门）预约下列考试的时间段。每场考试只能预约一个时间段。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px;"
    />

    <div v-if="examList.length === 0" style="text-align:center; color:#909399; padding:40px;">
      暂无可预约的考试
    </div>

    <el-card v-for="exam in examList" :key="exam.examId" style="margin-bottom: 16px;">
      <div slot="header" class="clearfix">
        <span style="font-weight: bold;">{{ exam.title }}</span>
        <span style="color:#909399; margin-left: 12px; font-size: 13px;">
          时长 {{ exam.totalTime }} 分钟 · 及格 {{ exam.qualifyScore }} 分
        </span>
        <el-tag v-if="exam.bookedSlotId" type="success" size="mini" style="margin-left: 12px;">本部门已预约</el-tag>
      </div>

      <el-table :data="exam.slots" border size="small">
        <el-table-column label="时间段" min-width="280">
          <template slot-scope="{ row }">
            {{ row.startTime }} ~ {{ row.endTime }}
          </template>
        </el-table-column>
        <el-table-column label="已约 / 上限" width="120" align="center">
          <template slot-scope="{ row }">
            {{ row.bookedCount }} / {{ row.maxDepart }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.status)" size="mini">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="{ row }">
            <!-- 本部门已约的那一段：取消 -->
            <el-button
              v-if="row.status === STATUS.MINE"
              type="text"
              @click="handleCancel(exam)"
            >取消预约</el-button>

            <!-- 可约段：本部门已在别处约则显示改约，否则预约 -->
            <el-button
              v-if="row.status === STATUS.BOOKABLE && !exam.bookedSlotId"
              type="text"
              @click="handleBook(row)"
            >预约</el-button>
            <el-button
              v-if="row.status === STATUS.BOOKABLE && exam.bookedSlotId"
              type="text"
              @click="handleChange(row)"
            >改约到此</el-button>

            <span v-if="row.status === STATUS.FULL" style="color:#909399;">已约满</span>
            <span v-if="row.status === STATUS.STARTED" style="color:#909399;">已截止</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

  </div>
</template>

<script>
import { fetchBookingExams, bookSlot, cancelBooking, changeBooking } from '@/api/exam/booking'

const STATUS = { BOOKABLE: 1, FULL: 2, STARTED: 3, MINE: 4 }

export default {
  name: 'BookingIndex',
  data() {
    return {
      STATUS,
      examList: []
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      fetchBookingExams().then(res => {
        this.examList = res.data || []
      })
    },
    statusText(status) {
      switch (status) {
        case STATUS.MINE: return '本部门已约'
        case STATUS.FULL: return '已约满'
        case STATUS.STARTED: return '已截止'
        default: return '可预约'
      }
    },
    statusTagType(status) {
      switch (status) {
        case STATUS.MINE: return 'success'
        case STATUS.FULL: return 'warning'
        case STATUS.STARTED: return 'info'
        default: return ''
      }
    },
    handleBook(slot) {
      this.$confirm('确认为本部门预约该时间段吗？', '提示', { type: 'warning' }).then(() => {
        bookSlot(slot.id).then(() => {
          this.$message.success('预约成功！')
          this.fetchData()
        })
      }).catch(() => {})
    },
    handleChange(slot) {
      this.$confirm('确认改约到该时间段吗？原预约将被取消。', '提示', { type: 'warning' }).then(() => {
        changeBooking(slot.id).then(() => {
          this.$message.success('改约成功！')
          this.fetchData()
        })
      }).catch(() => {})
    },
    handleCancel(exam) {
      this.$confirm('确认取消本部门在该考试的预约吗？', '提示', { type: 'warning' }).then(() => {
        cancelBooking(exam.examId).then(() => {
          this.$message.success('已取消预约！')
          this.fetchData()
        })
      }).catch(() => {})
    }
  }
}
</script>
