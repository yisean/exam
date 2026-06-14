<template>
  <div class="app-container">

    <h2 class="text-center">{{ paperData.title }} · 阅卷</h2>

    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="6" class="text-center">考生：{{ paperData.userId_dictText }}</el-col>
      <el-col :span="6" class="text-center">客观分：{{ paperData.objScore }}</el-col>
      <el-col :span="6" class="text-center">主观分（待评）：{{ subjTotal }}</el-col>
      <el-col :span="6" class="text-center">预计总分：{{ (paperData.objScore || 0) + subjTotal }} / {{ paperData.totalScore }}</el-col>
    </el-row>

    <el-alert
      :closable="false"
      title="客观题已自动判分；请为以下简答题逐题打分（0~该题满分的整数）与点评，整份提交后系统合分、判定并对考生公开。"
      type="info"
      style="margin: 16px 0;"
    />

    <el-card v-for="(item, idx) in simpleList" :key="item.id" style="margin-bottom: 16px;">

      <p><strong>{{ idx + 1 }}. {{ item.content }}（满分 {{ item.score }} 分）</strong></p>

      <el-alert :title="'考生作答：' + (item.answer || '（未作答）')" :closable="false" type="info" style="margin-bottom: 10px;" />
      <el-alert :title="'参考答案：' + referenceOf(item)" :closable="false" type="success" style="margin-bottom: 10px;" />

      <el-form label-width="90px">
        <el-form-item label="本题得分" required>
          <el-input-number v-model="scores[item.quId]" :min="0" :max="item.score" :step="1" :precision="0" />
          <span style="color:#909399; margin-left: 8px;">/ {{ item.score }} 分</span>
        </el-form-item>
        <el-form-item label="评语点评">
          <el-input
            v-model="comments[item.quId]"
            :rows="2"
            type="textarea"
            maxlength="200"
            show-word-limit
            placeholder="选填，提交后考生可见"
          />
        </el-form-item>
      </el-form>

    </el-card>

    <div style="margin-top: 20px">
      <el-button type="primary" @click="submit">提交评分</el-button>
      <el-button type="info" @click="onCancel">返回</el-button>
    </div>

  </div>
</template>

<script>
import { reviewDetail, submitReview } from '@/api/paper/exam'

export default {
  name: 'GradeExam',
  data() {
    return {
      paperId: '',
      paperData: {
        quList: []
      },
      simpleList: [],
      scores: {},
      comments: {}
    }
  },
  computed: {
    subjTotal() {
      let total = 0
      this.simpleList.forEach((item) => {
        const v = this.scores[item.quId]
        if (typeof v === 'number') {
          total += v
        }
      })
      return total
    }
  },
  created() {
    const id = this.$route.params.id
    if (typeof id !== 'undefined') {
      this.paperId = id
      this.fetchData(id)
    }
  },
  methods: {

    referenceOf(item) {
      return (item.answerList && item.answerList.length) ? item.answerList[0].content : ''
    },

    fetchData(id) {
      reviewDetail({ id: id }).then(response => {
        this.paperData = response.data
        const list = (this.paperData.quList || []).filter(it => it.quType === 4)
        const scores = {}
        const comments = {}
        list.forEach((it) => {
          scores[it.quId] = it.actualScore || 0
          comments[it.quId] = it.comment || ''
        })
        this.simpleList = list
        this.scores = scores
        this.comments = comments
      })
    },

    submit() {
      // 校验：每题得分为 0~该题满分 的整数
      for (let i = 0; i < this.simpleList.length; i++) {
        const item = this.simpleList[i]
        const v = this.scores[item.quId]
        if (typeof v !== 'number' || v < 0 || v > item.score) {
          this.$message({
            message: '第' + (i + 1) + '题得分须为 0~' + item.score + ' 的整数！',
            type: 'warning'
          })
          return
        }
      }

      const items = this.simpleList.map(item => ({
        quId: item.quId,
        score: this.scores[item.quId],
        comment: this.comments[item.quId]
      }))

      this.$confirm('确认提交评分？提交后试卷转为已完成、成绩对考生公开。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        submitReview({ paperId: this.paperId, items: items }).then(() => {
          this.$notify({
            title: '成功',
            message: '阅卷已提交！',
            type: 'success',
            duration: 2000
          })
          this.$router.go(-1)
        })
      }).catch(() => {})
    },

    onCancel() {
      this.$router.go(-1)
    }

  }
}
</script>

<style scoped>
  .text-center{
    text-align: center;
  }
</style>
