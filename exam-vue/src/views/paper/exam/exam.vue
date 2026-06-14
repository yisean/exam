<template>

  <div class="app-container">

    <el-row :gutter="24">

      <el-col :span="24">
        <el-card style="margin-bottom: 10px">

          距离考试结束还有：
          <exam-timer v-model="paperData.leftSeconds" @timeout="doHandler()" />

          <el-button :loading="loading" style="float: right; margin-top: -10px" type="primary" icon="el-icon-plus" @click="handHandExam()">
            {{ handleText }}
          </el-button>

        </el-card>
      </el-col>

      <el-col :span="5" :xs="24" style="margin-bottom: 10px">

        <el-card class="content-h">

          <p class="card-title">答题卡</p>
          <el-row :gutter="24" class="card-line" style="padding-left: 10px">
            <el-tag type="info">未作答</el-tag>
            <el-tag type="success">已作答</el-tag>
          </el-row>

          <div v-if="paperData.radioList!==undefined && paperData.radioList.length > 0">
            <p class="card-title">单选题</p>
            <el-row :gutter="24" class="card-line">
              <el-tag v-for="item in paperData.radioList" :key="item.quId" :type="cardItemClass(item.answered, item.quId)" @click="handSave(item)"> {{ item.sort+1 }}</el-tag>
            </el-row>
          </div>

          <div v-if="paperData.multiList!==undefined && paperData.multiList.length > 0">
            <p class="card-title">多选题</p>
            <el-row :gutter="24" class="card-line">
              <el-tag v-for="item in paperData.multiList" :key="item.quId" :type="cardItemClass(item.answered, item.quId)" @click="handSave(item)">{{ item.sort+1 }}</el-tag>
            </el-row>
          </div>

          <div v-if="paperData.judgeList!==undefined && paperData.judgeList.length > 0">
            <p class="card-title">判断题</p>
            <el-row :gutter="24" class="card-line">
              <el-tag v-for="item in paperData.judgeList" :key="item.quId" :type="cardItemClass(item.answered, item.quId)" @click="handSave(item)">{{ item.sort+1 }}</el-tag>
            </el-row>
          </div>

          <div v-if="paperData.uncertainList!==undefined && paperData.uncertainList.length > 0">
            <p class="card-title">不定项</p>
            <el-row :gutter="24" class="card-line">
              <el-tag v-for="item in paperData.uncertainList" :key="item.quId" :type="cardItemClass(item.answered, item.quId)" @click="handSave(item)">{{ item.sort+1 }}</el-tag>
            </el-row>
          </div>

          <div v-if="paperData.compositeList!==undefined && paperData.compositeList.length > 0">
            <p class="card-title">综合题</p>
            <div v-for="(c, ci) in paperData.compositeList" :key="c.id">
              <el-row :gutter="24" class="card-line">
                <span style="font-size:12px;color:#909399;">第{{ ci+1 }}大题：</span>
                <el-tag v-for="item in c.subList" :key="item.quId" :type="cardItemClass(item.answered, item.quId)" @click="handSave(item)">{{ item.sort+1 }}</el-tag>
              </el-row>
            </div>
          </div>

          <div v-if="paperData.saqList!==undefined && paperData.saqList.length > 0">
            <p class="card-title">简答题</p>
            <el-row :gutter="24" class="card-line">
              <el-tag v-for="item in paperData.saqList" :key="item.quId" :type="cardItemClass(item.answered, item.quId)" @click="handSave(item)">{{ item.sort+1 }}</el-tag>
            </el-row>
          </div>

        </el-card>

      </el-col>

      <el-col :span="19" :xs="24">

        <el-card class="qu-content content-h">
          <el-alert
            v-if="cardItem.materialContent"
            :title="cardItem.materialContent"
            :closable="false"
            type="info"
            style="margin-bottom: 12px;"
          />
          <p v-if="quData.content">{{ quData.sort + 1 }}.{{ quData.content }}</p>
          <p v-if="quData.image!=null && quData.image!=''">
            <el-image :src="quData.image" style="max-width:100%;" />
          </p>
          <div v-if="quData.quType === 1 || quData.quType===3">
            <el-radio-group v-model="radioValue">
              <el-radio v-for="item in quData.answerList" :key="item.id" :label="item.id">{{ item.abc }}.{{ item.content }}
                <div v-if="item.image!=null && item.image!=''" style="clear: both">
                  <el-image :src="item.image" style="max-width:100%;" />
                </div>
              </el-radio>
            </el-radio-group>
          </div>

          <div v-if="quData.quType === 2 || quData.quType === 5">

            <el-checkbox-group v-model="multiValue">
              <el-checkbox v-for="item in quData.answerList" :key="item.id" :label="item.id">{{ item.abc }}.{{ item.content }}
                <div v-if="item.image!=null && item.image!=''" style="clear: both">
                  <el-image :src="item.image" style="max-width:100%;" />
                </div>
              </el-checkbox>
            </el-checkbox-group>

          </div>

          <div v-if="quData.quType === 4">
            <el-input
              v-model="answerText"
              :rows="6"
              type="textarea"
              placeholder="请输入您的作答（本题为简答题，交卷后由老师人工评分）"
            />
          </div>

          <div style="margin-top: 20px">
            <el-button v-if="showPrevious" type="primary" icon="el-icon-back" @click="handPrevious()">
              上一题
            </el-button>

            <el-button v-if="showNext" type="warning" icon="el-icon-right" @click="handNext()">
              下一题
            </el-button>

          </div>

        </el-card>

      </el-col>

    </el-row>
  </div>

</template>

<script>
import { paperDetail, quDetail, handExam, fillAnswer } from '@/api/paper/exam'
import { Loading } from 'element-ui'
import ExamTimer from '@/views/paper/exam/components/ExamTimer'

export default {
  name: 'ExamProcess',
  components: { ExamTimer },
  data() {
    return {
      // 全屏/不全屏
      isFullscreen: false,
      showPrevious: false,
      showNext: true,
      loading: false,
      handleText: '交卷',
      pageLoading: false,
      // 试卷ID
      paperId: '',
      // 当前答题卡
      cardItem: {},
      allItem: [],
      // 当前题目内容
      quData: {
        answerList: []
      },
      // 试卷信息
      paperData: {
        leftSeconds: 99999,
        radioList: [],
        multiList: [],
        judgeList: [],
        uncertainList: [],
        compositeList: [],
        saqList: []
      },
      // 单选选定值
      radioValue: '',
      // 多选选定值
      multiValue: [],
      // 简答作答文本
      answerText: '',
      // 已答ID
      answeredIds: []
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

    // 答题卡样式
    cardItemClass(answered, quId) {
      if (quId === this.cardItem.quId) {
        return 'warning'
      }

      if (answered) {
        return 'success'
      }

      if (!answered) {
        return 'info'
      }
    },

    /**
     * 统计有多少题没答的
     * @returns {number}
     */
    countNotAnswered() {
      let notAnswered = 0

      this.paperData.radioList.forEach(function(item) {
        if (!item.answered) {
          notAnswered += 1
        }
      })

      this.paperData.multiList.forEach(function(item) {
        if (!item.answered) {
          notAnswered += 1
        }
      })

      this.paperData.judgeList.forEach(function(item) {
        if (!item.answered) {
          notAnswered += 1
        }
      })

      // 不定项 + 简答 + 综合题子题（综合题父题不作答）
      this.allItem.forEach(function(item) {
        if ((item.quType === 5 || item.quType === 4 || item.parentId) && !item.answered) {
          notAnswered += 1
        }
      })

      return notAnswered
    },

    /**
     * 下一题（按 allItem 顺序，综合题父题不在其中）
     */
    handNext() {
      const idx = this.allItem.findIndex(i => i.id === this.cardItem.id)
      if (idx >= 0 && idx < this.allItem.length - 1) {
        this.handSave(this.allItem[idx + 1])
      }
    },

    /**
     * 上一题
     */
    handPrevious() {
      const idx = this.allItem.findIndex(i => i.id === this.cardItem.id)
      if (idx > 0) {
        this.handSave(this.allItem[idx - 1])
      }
    },

    doHandler() {
      this.handleText = '正在交卷，请等待...'
      this.loading = true

      const params = { id: this.paperId }
      handExam(params).then(() => {
        this.$message({
          message: '试卷提交成功，即将进入试卷详情！',
          type: 'success'
        })

        this.$router.push({ name: 'ShowExam', params: { id: this.paperId }})
      })
    },

    // 交卷操作
    handHandExam() {
      const that = this

      // 交卷保存答案
      this.handSave(this.cardItem, function() {
        const notAnswered = that.countNotAnswered()

        let msg = '确认要交卷吗？'

        if (notAnswered > 0) {
          msg = '您还有' + notAnswered + '题未作答，确认要交卷吗?'
        }

        that.$confirm(msg, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          that.doHandler()
        }).catch(() => {
          that.$message({
            type: 'info',
            message: '交卷已取消，您可以继续作答！'
          })
        })
      })
    },

    // 保存答案
    handSave(item, callback) {
      if (item.id === this.allItem[0].id) {
        this.showPrevious = false
      } else {
        this.showPrevious = true
      }

      // 最后一个索引
      const last = this.allItem.length - 1

      if (item.id === this.allItem[last].id) {
        this.showNext = false
      } else {
        this.showNext = true
      }

      // 简答题以文本作答；客观题以选项ID数组作答
      let answers = []
      let answer = ''
      if (this.cardItem.quType === 4) {
        answer = this.answerText
      } else {
        answers = this.multiValue.slice()
        if (this.radioValue !== '') {
          answers.push(this.radioValue)
        }
      }

      const params = { paperId: this.paperId, quId: this.cardItem.quId, answers: answers, answer: answer }
      fillAnswer(params).then(() => {
        // 已答判定：客观题选了项、或简答题作答文本非空
        this.cardItem.answered = answers.length > 0 || (!!answer && answer.trim() !== '')

        // 最后一个动作，交卷
        if (callback) {
          callback()
        }

        // 查找详情
        this.fetchQuData(item)
      })
    },

    // 试卷详情
    fetchQuData(item) {
      // 打开
      const loading = Loading.service({
        text: '拼命加载中',
        background: 'rgba(0, 0, 0, 0.7)'
      })

      // 获得详情
      this.cardItem = item

      // 查找下个详情
      const params = { paperId: this.paperId, quId: item.quId }
      quDetail(params).then(response => {
        console.log(response)
        this.quData = response.data
        this.radioValue = ''
        this.multiValue = []
        this.answerText = ''

        // 简答题：回填考生已作答文本
        if (this.quData.quType === 4) {
          this.answerText = this.quData.answer || ''
        }

        // 填充该题目的答案
        this.quData.answerList.forEach((item) => {
          if ((this.quData.quType === 1 || this.quData.quType === 3) && item.checked) {
            this.radioValue = item.id
          }

          if ((this.quData.quType === 2 || this.quData.quType === 5) && item.checked) {
            this.multiValue.push(item.id)
          }
        })

        // 关闭详情
        loading.close()
      })
    },

    // 试卷详情
    fetchData(id) {
      const params = { id: id }
      paperDetail(params).then(response => {
        // 试卷内容
        this.paperData = response.data

        const that = this

        if (this.paperData.radioList) {
          this.paperData.radioList.forEach(function(item) { that.allItem.push(item) })
        }
        if (this.paperData.multiList) {
          this.paperData.multiList.forEach(function(item) { that.allItem.push(item) })
        }
        if (this.paperData.judgeList) {
          this.paperData.judgeList.forEach(function(item) { that.allItem.push(item) })
        }
        if (this.paperData.uncertainList) {
          this.paperData.uncertainList.forEach(function(item) { that.allItem.push(item) })
        }
        // 综合题：把5个子题加入作答序列，并携带共享材料
        if (this.paperData.compositeList) {
          this.paperData.compositeList.forEach(function(c) {
            const subs = c.subList || []
            subs.forEach(function(sub) {
              sub.materialContent = c.content
              that.allItem.push(sub)
            })
          })
        }
        // 简答题：加入作答序列
        if (this.paperData.saqList) {
          this.paperData.saqList.forEach(function(item) { that.allItem.push(item) })
        }

        // 获得第一题内容
        if (this.allItem.length > 0) {
          this.cardItem = this.allItem[0]
          this.fetchQuData(this.cardItem)
        }
      })
    }

  }
}
</script>

<style scoped>

  .qu-content div{
    line-height: 30px;
    width: 100%;
  }

  .el-checkbox-group label,.el-radio-group label{
    width: 100%;
  }

  .content-h{
    height: calc(100vh - 110px);
    overflow-y: auto;
  }

  .card-title{
    background: #eee;
    line-height: 35px;
    text-align: center;
    font-size: 14px;
  }
  .card-line{
    padding-left: 10px
  }
  .card-line span {
    cursor: pointer;
    margin: 2px;
  }

  ::v-deep
  .el-radio, .el-checkbox{
    padding: 9px 20px 9px 10px;
    border-radius: 4px;
    border: 1px solid #dcdfe6;
    margin-bottom: 10px;
    width: 100%;
  }

  .is-checked{
    border: #409eff 1px solid;
  }

  .el-radio img, .el-checkbox img{
    max-width: 200px;
    max-height: 200px;
    border: #dcdfe6 1px dotted;
  }

  ::v-deep
  .el-checkbox__inner {
    display: none;
  }

  ::v-deep
  .el-radio__inner{
    display: none;
  }

  ::v-deep
  .el-checkbox__label{
    line-height: 30px;
  }

  ::v-deep
  .el-radio__label{
    line-height: 30px;
  }

</style>

