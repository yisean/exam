<template>
  <div class="app-container">

    <h3>组卷信息</h3>
    <el-card style="margin-top: 20px">

      <div style="float: right; font-weight: bold; color: #ff0000">试卷总分：{{ postForm.totalScore }}分</div>

      <div>

        <el-button class="filter-item" size="small" type="primary" icon="el-icon-plus" @click="handleAdd">
          添加题库
        </el-button>

        <el-table
          :data="repoList"
          :border="false"
          empty-text="请点击上面的`添加题库`进行设置"
          style="width: 100%; margin-top: 15px"
        >
          <el-table-column
            label="题库"
            width="200"
          >
            <template v-slot="scope">
              <repo-select
                v-model="scope.row.repoId"
                :multi="false"
                :excludes="excludes"
                @change="repoChange($event, scope.row)" />
            </template>

          </el-table-column>
          <el-table-column
            label="单选数量"
            align="center"
          >

            <template v-slot="scope">
              <el-input-number v-model="scope.row.radioCount" :min="0" :max="scope.row.totalRadio" :controls="false" style="width: 100px" /> / {{ scope.row.totalRadio }}
            </template>

          </el-table-column>

          <el-table-column
            label="单选分数"
            align="center"
          >
            <template v-slot="scope">
              <el-input-number v-model="scope.row.radioScore" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>

          <el-table-column
            label="多选数量"
            align="center"
          >

            <template v-slot="scope">
              <el-input-number v-model="scope.row.multiCount" :min="0" :max="scope.row.totalMulti" :controls="false" style="width: 100px" /> / {{ scope.row.totalMulti }}
            </template>

          </el-table-column>

          <el-table-column
            label="多选分数"
            align="center"
          >
            <template v-slot="scope">
              <el-input-number v-model="scope.row.multiScore" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>

          <el-table-column
            label="判断题数量"
            align="center"
          >

            <template v-slot="scope">
              <el-input-number v-model="scope.row.judgeCount" :min="0" :max="scope.row.totalJudge" :controls="false" style="width: 100px" />  / {{ scope.row.totalJudge }}
            </template>

          </el-table-column>

          <el-table-column
            label="判断题分数"
            align="center"
          >
            <template v-slot="scope">
              <el-input-number v-model="scope.row.judgeScore" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>

          <el-table-column
            label="删除"
            align="center"
            width="80px"
          >
            <template v-slot="scope">
              <el-button type="danger" icon="el-icon-delete" circle @click="removeItem(scope.$index)" />
            </template>
          </el-table-column>

        </el-table>

      </div>

    </el-card>

    <h3>考试配置</h3>
    <el-card style="margin-top: 20px">

      <el-form ref="postForm" :model="postForm" :rules="rules" label-position="left" label-width="120px">

        <el-form-item label="考试名称" prop="title">
          <el-input v-model="postForm.title" />
        </el-form-item>

        <el-form-item label="考试描述" prop="content">
          <el-input v-model="postForm.content" type="textarea" />
        </el-form-item>

        <el-form-item label="总分数" prop="totalScore">
          <el-input-number :value="postForm.totalScore" disabled />
        </el-form-item>

        <el-form-item label="及格分" prop="qualifyScore">
          <el-input-number v-model="postForm.qualifyScore" :max="postForm.totalScore" />
        </el-form-item>

        <el-form-item label="考试时长(分钟)" prop="totalTime">
          <el-input-number v-model="postForm.totalTime" />
        </el-form-item>

        <el-form-item label="是否限时">
          <el-checkbox v-model="postForm.timeLimit" />
        </el-form-item>

        <el-form-item v-if="postForm.timeLimit" label="考试时间" prop="totalTime">

          <el-date-picker
            v-model="dateValues"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            type="daterange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />

        </el-form-item>

      </el-form>

    </el-card>

    <h3>权限配置</h3>
    <el-card style="margin-top: 20px;">

      <el-radio-group v-model="postForm.openType" style="margin-bottom: 20px">
        <el-radio :label="1" border>完全公开</el-radio>
        <el-radio :label="2" border>部门开放</el-radio>
      </el-radio-group>

      <el-alert
        v-if="postForm.openType===1"
        title="开放的，任何人都可以进行考试！"
        type="warning"
      />

      <div v-if="postForm.openType===2">
        <el-input
          v-model="filterText"
          placeholder="输入关键字进行过滤"
        />

        <el-tree

          v-loading="treeLoading"
          ref="tree"
          :data="treeData"
          :default-checked-keys="postForm.departIds"
          :props="defaultProps"
          :filter-node-method="filterNode"
          empty-text=" "
          default-expand-all
          show-checkbox
          node-key="id"
          @check-change="handleCheckChange"
        />

      </div>

    </el-card>

    <h3>时间段配置</h3>
    <el-card style="margin-top: 20px;">

      <el-alert
        title="可为考试配置一个或多个时间段，每段独立设置开放类型与起止时间。配置了时间段后，学员的可见与可考以时间段为准（不再使用上方的权限配置）。"
        type="info"
        :closable="false"
        style="margin-bottom: 15px;"
      />

      <el-button size="small" type="primary" icon="el-icon-plus" @click="addSlot">添加时间段</el-button>

      <el-table :data="postForm.timeSlots" border style="width: 100%; margin-top: 15px;" empty-text="未配置时间段">
        <el-table-column label="开放类型" width="110" align="center">
          <template v-slot="scope">{{ slotTypeText(scope.row.openType) }}</template>
        </el-table-column>
        <el-table-column label="时间窗口" min-width="260" align="center">
          <template v-slot="scope">{{ scope.row.startTime }} ~ {{ scope.row.endTime }}</template>
        </el-table-column>
        <el-table-column label="可预约部门数" width="120" align="center">
          <template v-slot="scope">{{ scope.row.openType === 3 ? scope.row.maxDepart : '-' }}</template>
        </el-table-column>
        <el-table-column label="部门" min-width="150" align="center">
          <template v-slot="scope">
            <span v-if="scope.row.openType === 2">指定 {{ (scope.row.departIds || []).length }} 个部门</span>
            <span v-else-if="scope.row.openType === 3">免约 {{ (scope.row.freeDepartIds || []).length }} 个部门</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template v-slot="scope">
            <el-button type="text" @click="editSlot(scope.$index)">编辑</el-button>
            <el-button
              v-if="scope.row.openType === 3 && scope.row.id"
              type="text"
              @click="viewBookings(scope.row)"
            >查看预约</el-button>
            <el-button type="text" style="color:#f56c6c;" @click="removeSlot(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

    </el-card>

    <div style="margin-top: 20px">
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>

    <el-dialog :visible.sync="slotDialog" title="时间段配置" width="600px" append-to-body>
      <el-form label-width="110px">
        <el-form-item label="开放类型">
          <el-radio-group v-model="slotForm.openType">
            <el-radio :label="1">不限人员</el-radio>
            <el-radio :label="2">指定部门</el-radio>
            <el-radio :label="3">预约考试</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="起止时间">
          <el-date-picker
            v-model="slotForm._range"
            type="datetimerange"
            format="yyyy-MM-dd HH:mm"
            value-format="yyyy-MM-dd HH:mm"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item v-if="slotForm.openType === 3" label="可预约部门数">
          <el-input-number v-model="slotForm.maxDepart" :min="1" />
        </el-form-item>
        <el-form-item v-if="slotForm.openType === 2" label="可考部门">
          <el-tree
            ref="slotTree"
            :data="treeData"
            :props="defaultProps"
            node-key="id"
            show-checkbox
            default-expand-all
            empty-text=" "
          />
        </el-form-item>
        <el-form-item v-if="slotForm.openType === 3" label="免约部门">
          <el-tree
            ref="slotTree"
            :data="treeData"
            :props="defaultProps"
            node-key="id"
            show-checkbox
            default-expand-all
            empty-text=" "
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="slotDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSlot">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="bookingDialog" :title="`时间段预约情况（已约 ${bookingList.length} 个部门）`" width="600px" append-to-body>
      <el-table :data="bookingList" border size="small" empty-text="暂无部门预约">
        <el-table-column label="部门" prop="departName" min-width="140" />
        <el-table-column label="预约人" prop="userName" width="120" align="center" />
        <el-table-column label="预约时间" prop="createTime" width="150" align="center" />
        <el-table-column label="操作" width="90" align="center">
          <template v-slot="scope">
            <el-button type="text" style="color:#f56c6c;" @click="cancelBooking(scope.row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script>
import { fetchDetail, saveData } from '@/api/exam/exam'
import { fetchSlotBookings, adminCancelBooking } from '@/api/exam/booking'
import { fetchTree } from '@/api/sys/depart/depart'
import RepoSelect from '@/components/RepoSelect'

export default {
  name: 'ExamDetail',
  components: { RepoSelect },
  data() {
    return {

      treeData: [],
      defaultProps: {
        label: 'deptName'
      },
      filterText: '',
      treeLoading: false,
      dateValues: [],
      // 题库
      repoList: [],
      // 已选择的题库
      excludes: [],
      postForm: {
        // 总分数
        totalScore: 0,
        // 题库列表
        repoList: [],
        // 开放类型
        openType: 1,
        // 考试班级列表
        departIds: [],
        // 时间段列表
        timeSlots: []
      },
      // 时间段编辑对话框
      slotDialog: false,
      editingSlotIndex: -1,
      // 预约查看对话框
      bookingDialog: false,
      bookingSlotId: '',
      bookingList: [],
      slotForm: {
        openType: 1,
        _range: [],
        maxDepart: 1,
        departIds: [],
        freeDepartIds: []
      },
      rules: {
        title: [
          { required: true, message: '考试名称不能为空！' }
        ],

        content: [
          { required: true, message: '考试名称不能为空！' }
        ],

        open: [
          { required: true, message: '考试权限不能为空！' }
        ],

        totalScore: [
          { required: true, message: '考试分数不能为空！' }
        ],

        qualifyScore: [
          { required: true, message: '及格分不能为空！' }
        ],

        totalTime: [
          { required: true, message: '考试时间不能为空！' }
        ],

        ruleId: [
          { required: true, message: '考试规则不能为空' }
        ],
        password: [
          { required: true, message: '考试口令不能为空！' }
        ]
      }
    }
  },

  watch: {

    filterText(val) {
      this.$refs.tree.filter(val)
    },

    dateValues: {

      handler() {
        this.postForm.startTime = this.dateValues[0]
        this.postForm.endTime = this.dateValues[1]
      }
    },

    // 题库变换
    repoList: {

      handler(val) {
        let totalScore = 0
        this.excludes = []
        for (let i = 0; i<val.length; i++) {
          const item = val[i]
          if (item.radioCount > 0 && item.radioScore>0) {
            totalScore += item.radioCount * item.radioScore
          }

          if (item.multiCount>0 && item.multiScore>0) {
            totalScore += item.multiCount * item.multiScore
          }

          if (item.judgeCount>0 && item.judgeScore>0) {
            totalScore += item.judgeCount * item.judgeScore
          }
          this.excludes.push(item.id)
        }

        // 赋值
        this.postForm.totalScore = totalScore
        this.postForm.repoList = val
        this.$forceUpdate()
      },
      deep: true
    }

  },
  created() {
    const id = this.$route.params.id
    if (typeof id !== undefined) {
      this.fetchData(id)
    }

    fetchTree({}).then(response => {
      this.treeData = response.data
    })
  },
  methods: {

    handleSave() {
      this.$refs.postForm.validate((valid) => {
        if (!valid) {
          return
        }

        if (this.postForm.totalScore === 0) {
          this.$notify({
            title: '提示信息',
            message: '考试规则设置不正确，请确认！',
            type: 'warning',
            duration: 2000
          })

          return
        }

        for (let i = 0; i < this.postForm.repoList.length; i++) {
          const repo = this.postForm.repoList[i]
          if (!repo.repoId) {
            this.$notify({
              title: '提示信息',
              message: '考试题库选择不正确！',
              type: 'warning',
              duration: 2000
            })
            return
          }

          if ((repo.radioCount > 0 && repo.radioScore === 0) || (repo.radioCount === 0 && repo.radioScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的单选题配置！',
              type: 'warning',
              duration: 2000
            })

            return
          }

          if ((repo.multiCount > 0 && repo.multiScore === 0) || (repo.multiCount === 0 && repo.multiScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的多选题配置！',
              type: 'warning',
              duration: 2000
            })

            return
          }

          if ((repo.judgeCount > 0 && repo.judgeScore === 0) || (repo.judgeCount === 0 && repo.judgeScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的判断题配置！',
              type: 'warning',
              duration: 2000
            })
            return
          }
        }

        this.$confirm('确实要提交保存吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.submitForm()
        })
      })
    },

    handleCheckChange() {
      const that = this
      // 置空
      this.postForm.departIds = []
      const nodes = this.$refs.tree.getCheckedNodes()
      nodes.forEach(function(item) {
        that.postForm.departIds.push(item.id)
      })
    },

    // 添加子项
    handleAdd() {
      this.repoList.push({ id: '', rowId: new Date().getTime(), radioCount: 0, radioScore: 0, multiCount: 0, multiScore: 0, judgeCount: 0, judgeScore: 0, saqCount: 0, saqScore: 0 })
    },

    removeItem(index) {
      this.repoList.splice(index, 1)
    },

    fetchData(id) {
      fetchDetail(id).then(response => {
        this.postForm = response.data

        if (this.postForm.startTime && this.postForm.endTime) {
          this.dateValues[0] = this.postForm.startTime
          this.dateValues[1] = this.postForm.endTime
        }
        this.repoList = this.postForm.repoList
      })
    },

    submitForm() {
      // 校验和处理数据
      this.postForm.repoList = this.repoList

      saveData(this.postForm).then(() => {
        this.$notify({
          title: '成功',
          message: '考试保存成功！',
          type: 'success',
          duration: 2000
        })

        this.$router.push({ name: 'ListExam' })
      })
    },

    filterNode(value, data) {
      if (!value) return true
      return data.deptName.indexOf(value) !== -1
    },

    repoChange(e, row) {
      // 赋值ID
      row.id = e.id

      if (e != null) {
        row.totalRadio = e.radioCount
        row.totalMulti = e.multiCount
        row.totalJudge = e.judgeCount
      } else {
        row.totalRadio = 0
        row.totalMulti = 0
        row.totalJudge = 0
      }
    },

    // ----------------- 时间段配置 -----------------

    slotTypeText(type) {
      switch (type) {
        case 2: return '指定部门'
        case 3: return '预约考试'
        default: return '不限人员'
      }
    },

    addSlot() {
      this.editingSlotIndex = -1
      this.slotForm = { openType: 1, _range: [], maxDepart: 1, departIds: [], freeDepartIds: [] }
      this.slotDialog = true
      this.$nextTick(() => {
        if (this.$refs.slotTree) {
          this.$refs.slotTree.setCheckedKeys([])
        }
      })
    },

    editSlot(index) {
      const slot = this.postForm.timeSlots[index]
      this.editingSlotIndex = index
      this.slotForm = {
        openType: slot.openType,
        _range: slot.startTime && slot.endTime ? [slot.startTime, slot.endTime] : [],
        maxDepart: slot.maxDepart || 1,
        departIds: (slot.departIds || []).slice(),
        freeDepartIds: (slot.freeDepartIds || []).slice()
      }
      this.slotDialog = true
      this.$nextTick(() => {
        if (this.$refs.slotTree) {
          const checked = this.slotForm.openType === 2 ? this.slotForm.departIds : this.slotForm.freeDepartIds
          this.$refs.slotTree.setCheckedKeys(checked)
        }
      })
    },

    saveSlot() {
      const f = this.slotForm
      if (!f._range || f._range.length !== 2) {
        this.$message.warning('请选择起止时间！')
        return
      }
      if (f.openType === 3 && (!f.maxDepart || f.maxDepart <= 0)) {
        this.$message.warning('预约型时间段必须设置可预约部门数上限！')
        return
      }

      let departIds = []
      let freeDepartIds = []
      if (f.openType === 2) {
        departIds = this.$refs.slotTree ? this.$refs.slotTree.getCheckedKeys() : []
        if (departIds.length === 0) {
          this.$message.warning('指定部门型时间段必须至少选择一个部门！')
          return
        }
      } else if (f.openType === 3) {
        freeDepartIds = this.$refs.slotTree ? this.$refs.slotTree.getCheckedKeys() : []
      }

      const slot = {
        openType: f.openType,
        startTime: f._range[0],
        endTime: f._range[1],
        maxDepart: f.openType === 3 ? f.maxDepart : 0,
        departIds: departIds,
        freeDepartIds: freeDepartIds
      }

      if (this.editingSlotIndex >= 0) {
        this.postForm.timeSlots.splice(this.editingSlotIndex, 1, slot)
      } else {
        this.postForm.timeSlots.push(slot)
      }
      this.slotDialog = false
    },

    removeSlot(index) {
      this.postForm.timeSlots.splice(index, 1)
    },

    // 查看某预约型时段的预约情况
    viewBookings(slot) {
      this.bookingSlotId = slot.id
      this.bookingDialog = true
      this.loadBookings()
    },

    loadBookings() {
      fetchSlotBookings(this.bookingSlotId).then(res => {
        this.bookingList = res.data || []
      })
    },

    // 管理员取消某部门预约（已有学员试卷时二次确认）
    cancelBooking(row) {
      const tip = row.hasPaper
        ? `【${row.departName}】已有学员开考或交卷，取消预约不会删除已有成绩，但该部门尚未开考的学员将无法再考。确认取消？`
        : `确认取消【${row.departName}】的预约吗？`
      this.$confirm(tip, '提示', { type: 'warning' }).then(() => {
        adminCancelBooking(row.id).then(() => {
          this.$message.success('已取消该部门预约！')
          this.loadBookings()
        })
      }).catch(() => {})
    }

  }
}
</script>

