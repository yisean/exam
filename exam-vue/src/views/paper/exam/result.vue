<template>
  <div class="app-container">

    <h2 class="text-center">{{ paperData.title }}</h2>
    <p class="text-center" style="color: #666">{{ paperData.createTime }}</p>

    <el-row :gutter="24" style="margin-top: 50px">

      <el-col :span="8" class="text-center">
        考生姓名：{{ paperData.userId_dictText }}
      </el-col>

      <el-col :span="8" class="text-center">
        考试用时：{{ paperData.userTime }}分钟
      </el-col>

      <el-col :span="8" class="text-center">
        考试得分：<span v-if="paperData.state === 1" style="color:#e6a23c">待阅卷</span><span v-else>{{ paperData.userScore }}</span>
      </el-col>

    </el-row>

    <el-alert
      v-if="paperData.state === 1"
      :closable="false"
      title="本试卷含简答题，正在等待老师阅卷，成绩与点评待评定后可见。"
      type="warning"
      style="margin-top: 16px;"
    />

    <el-card style="margin-top: 20px">

      <div v-for="item in paperData.quList" :key="item.id" :style="item.parentId ? 'padding-left: 20px; border-left: 3px solid #ebeef5;' : ''" class="qu-content">

        <template v-if="item.quType === 6">
          <p><strong>{{ item.sort + 1 }}. 【综合题】（小计：{{ compositeScores[item.id] }} / {{ compositeMax[item.id] }}）</strong></p>
          <el-alert :title="item.content" :closable="false" type="info" style="margin-bottom: 10px;" />
        </template>

        <template v-else>
          <p>
            {{ item.sort + 1 }}.{{ item.content }}（得分：{{ item.actualScore }}<span v-if="item.quType===5 && item.answered && !item.isRight && item.actualScore>0">·部分对</span>）
          </p>
          <p v-if="item.image!=null && item.image!=''">
            <el-image :src="item.image" style="max-width:100%;" />
          </p>
        </template>

        <div v-if="item.quType === 1 || item.quType===3">
          <el-radio-group v-model="radioValues[item.id]">
            <el-radio v-for="an in item.answerList" :key="an.id" :label="an.id">
              {{ an.abc }}.{{ an.content }}
              <div v-if="an.image!=null && an.image!=''" style="clear: both">
                <el-image :src="an.image" style="max-width:100%;" />
              </div>
            </el-radio>
          </el-radio-group>

          <el-row :gutter="24">

            <el-col :span="12" style="color: #24da70">
              正确答案：{{ radioRights[item.id] }}
            </el-col>

            <el-col v-if="!item.answered" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：未答
            </el-col>

            <el-col v-if="item.answered && !item.isRight" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：{{ myRadio[item.id] }}
            </el-col>

            <el-col v-if="item.answered && item.isRight" :span="12" style="text-align: right; color: #24da70;">
              答题结果：{{ myRadio[item.id] }}
            </el-col>

          </el-row>

        </div>

        <div v-if="item.quType === 4">

          <el-row :gutter="24">

            <el-col :span="24" style="margin-bottom: 8px;">
              我的回答：{{ item.answer }}
            </el-col>

            <el-col :span="24" style="color: #24da70; margin-bottom: 8px;">
              参考答案：{{ (item.answerList && item.answerList.length) ? item.answerList[0].content : '' }}
            </el-col>

            <el-col v-if="item.comment" :span="24" style="color: #e6a23c;">
              老师点评：{{ item.comment }}
            </el-col>

          </el-row>

        </div>

        <div v-if="item.quType === 2 || item.quType === 5">
          <el-checkbox-group v-model="multiValues[item.id]">
            <el-checkbox v-for="an in item.answerList" :key="an.id" :label="an.id">{{ an.abc }}.{{ an.content }}
              <div v-if="an.image!=null && an.image!=''" style="clear: both">
                <el-image :src="an.image" style="max-width:100%;" />
              </div>
            </el-checkbox>
          </el-checkbox-group>

          <el-row :gutter="24">

            <el-col :span="12" style="color: #24da70">
              正确答案：{{ multiRights[item.id].join(',') }}
            </el-col>

            <el-col v-if="!item.answered" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：未答
            </el-col>

            <el-col v-if="item.answered && !item.isRight" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：{{ myMulti[item.id].join(',') }}
            </el-col>

            <el-col v-if="item.answered && item.isRight" :span="12" style="text-align: right; color: #24da70;">
              答题结果：{{ myMulti[item.id].join(',') }}
            </el-col>

          </el-row>
        </div>

      </div>

    </el-card>

  </div>
</template>

<script>

import { paperResult } from '@/api/paper/exam'

export default {
  data() {
    return {
      // 试卷ID
      paperId: '',
      paperData: {
        quList: []
      },
      radioValues: {},
      multiValues: {},
      radioRights: {},
      multiRights: {},
      myRadio: {},
      myMulti: {},
      // 综合题小计：父题id -> 子题实得分之和 / 满分之和
      compositeScores: {},
      compositeMax: {}
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

    fetchData(id) {
      const params = { id: id }
      paperResult(params).then(response => {
        // 试卷内容
        this.paperData = response.data

        // 综合题小计：父题id -> 子题实得分/满分之和
        const cScore = {}
        const cMax = {}
        this.paperData.quList.forEach((it) => {
          if (it.quType === 6) {
            cScore[it.id] = 0
            cMax[it.id] = 0
          }
        })
        this.paperData.quList.forEach((it) => {
          if (it.parentId) {
            cScore[it.parentId] = (cScore[it.parentId] || 0) + (it.actualScore || 0)
            cMax[it.parentId] = (cMax[it.parentId] || 0) + (it.score || 0)
          }
        })
        this.compositeScores = cScore
        this.compositeMax = cMax

        // 填充该题目的答案
        this.paperData.quList.forEach((item) => {
          let radioValue = ''
          let radioRight = ''
          let myRadio = ''
          const multiValue = []
          const multiRight = []
          const myMulti = []
          const answerList = item.answerList || []

          answerList.forEach((an) => {
            // 用户选定的
            if (an.checked) {
              if (item.quType === 1 || item.quType === 3) {
                radioValue = an.id
                myRadio = an.abc
              } else {
                multiValue.push(an.id)
                myMulti.push(an.abc)
              }
            }

            // 正确答案
            if (an.isRight) {
              if (item.quType === 1 || item.quType === 3) {
                radioRight = an.abc
              } else {
                multiRight.push(an.abc)
              }
            }
          })

          this.multiValues[item.id] = multiValue
          this.radioValues[item.id] = radioValue

          this.radioRights[item.id] = radioRight
          this.multiRights[item.id] = multiRight

          this.myRadio[item.id] = myRadio
          this.myMulti[item.id] = myMulti
        })

        console.log(this.multiValues)
        console.log(this.radioValues)
      })
    }
  }
}
</script>

<style scoped>

  .qu-content{

    border-bottom: #eee 1px solid;
    padding-bottom: 10px;

  }

  .qu-content div{
    line-height: 30px;
  }

  .el-checkbox-group label,.el-radio-group label{
    width: 100%;
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

</style>

