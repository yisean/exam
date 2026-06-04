<template>
  <div class="app-container">

    <el-form ref="postForm" :model="postForm" :rules="rules" label-position="left" label-width="120px">

      <el-card>
        <div slot="header">基本信息</div>

        <el-form-item label="题目类型">
          <el-tag type="success">综合题</el-tag>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">综合题含 5 个子题，分值为各子题之和。</span>
        </el-form-item>

        <el-form-item label="难度等级" prop="level">
          <el-select v-model="postForm.level" class="filter-item">
            <el-option v-for="item in levels" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="归属题库" prop="repoIds">
          <repo-select v-model="postForm.repoIds" :multi="true" />
        </el-form-item>

        <el-form-item label="共享材料" prop="content">
          <el-input v-model="postForm.content" :rows="4" type="textarea" placeholder="供 5 个子题共用的题干 / 材料" />
        </el-form-item>

        <el-form-item label="材料图片">
          <file-upload v-model="postForm.image" accept=".jpg,.jepg,.png" />
        </el-form-item>
      </el-card>

      <el-card style="margin-top: 20px;">
        <div slot="header">
          子题设置（共 5 个，不可增减）
          <span style="float: right; font-weight: bold;">综合题总分：{{ totalScore }} 分</span>
        </div>

        <el-alert
          :closable="false"
          title="每个子题按其题型录入：单选恰 1 个正确答案、判断为正确/错误两项、多选与不定项 1 个及以上正确答案。子题不能再是综合题。"
          type="warning"
          style="margin-bottom: 15px;"
        />

        <el-card v-for="(sub, sIndex) in postForm.subQuList" :key="sIndex" shadow="never" style="margin-bottom: 15px;">

          <div slot="header">
            <span style="font-weight: bold;">子题 {{ sIndex + 1 }}</span>
            <span style="margin-left: 20px;">题型：</span>
            <el-radio-group v-model="sub.quType" size="mini" @change="handleSubTypeChange(sub)">
              <el-radio-button v-for="t in subTypes" :key="t.value" :label="t.value">{{ t.label }}</el-radio-button>
            </el-radio-group>
            <span style="float: right;">
              分值：
              <el-input-number v-model="sub.score" :min="1" :controls="false" size="mini" style="width: 80px;" /> 分
            </span>
          </div>

          <el-form-item label="子题题干">
            <el-input v-model="sub.content" :rows="2" type="textarea" />
          </el-form-item>

          <el-button
            v-if="sub.quType !== 3"
            type="primary"
            icon="el-icon-plus"
            size="mini"
            plain
            style="margin-bottom: 10px;"
            @click="addOption(sub)"
          >添加选项</el-button>

          <el-table :data="sub.answerList" :border="true" size="small" style="width: 100%;">
            <el-table-column label="是否答案" width="100" align="center">
              <template v-slot="scope">
                <el-checkbox v-model="scope.row.isRight">答案</el-checkbox>
              </template>
            </el-table-column>
            <el-table-column label="答案内容">
              <template v-slot="scope">
                <el-input v-model="scope.row.content" :rows="1" type="textarea" />
              </template>
            </el-table-column>
            <el-table-column label="答案解析">
              <template v-slot="scope">
                <el-input v-model="scope.row.analysis" :rows="1" type="textarea" />
              </template>
            </el-table-column>
            <el-table-column v-if="sub.quType !== 3" label="操作" align="center" width="80">
              <template v-slot="scope">
                <el-button type="danger" icon="el-icon-delete" circle size="mini" @click="removeOption(sub, scope.$index)" />
              </template>
            </el-table-column>
          </el-table>

          <el-form-item label="子题解析" style="margin-top: 10px;">
            <el-input v-model="sub.analysis" :rows="1" type="textarea" />
          </el-form-item>

        </el-card>

      </el-card>

      <div style="margin-top: 20px;">
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button type="info" @click="onCancel">返回</el-button>
      </div>

    </el-form>

  </div>
</template>

<script>
import { fetchDetail, saveData } from '@/api/qu/qu'
import RepoSelect from '@/components/RepoSelect'
import FileUpload from '@/components/FileUpload'

export default {
  name: 'QuCompositeDetail',
  components: { FileUpload, RepoSelect },
  data() {
    return {
      levels: [
        { value: 1, label: '普通' },
        { value: 2, label: '较难' }
      ],
      // 子题题型：单选/多选/判断/不定项（不含综合题）
      subTypes: [
        { value: 1, label: '单选' },
        { value: 2, label: '多选' },
        { value: 3, label: '判断' },
        { value: 5, label: '不定项' }
      ],
      postForm: {
        quType: 6,
        level: 1,
        image: '',
        content: '',
        repoIds: [],
        subQuList: []
      },
      rules: {
        content: [{ required: true, message: '共享材料不能为空！' }],
        level: [{ required: true, message: '必须选择难度等级！' }],
        repoIds: [{ required: true, message: '至少要选择一个题库！' }]
      }
    }
  },
  computed: {
    totalScore() {
      return this.postForm.subQuList.reduce((sum, s) => sum + (s.score || 0), 0)
    }
  },
  created() {
    const id = this.$route.params.id
    if (typeof id !== 'undefined') {
      this.fetchData(id)
    } else {
      this.initSubList()
    }
  },
  methods: {

    // 初始化5个空子题
    initSubList() {
      const list = []
      for (let i = 0; i < 5; i++) {
        list.push(this.buildSub())
      }
      this.postForm.subQuList = list
    },

    buildSub() {
      return {
        quType: 1,
        score: 2,
        content: '',
        analysis: '',
        image: '',
        answerList: [
          { isRight: false, content: '', analysis: '' },
          { isRight: false, content: '', analysis: '' },
          { isRight: false, content: '', analysis: '' },
          { isRight: false, content: '', analysis: '' }
        ]
      }
    },

    // 子题题型切换：重置选项（判断题固定正确/错误两项）
    handleSubTypeChange(sub) {
      if (sub.quType === 3) {
        sub.answerList = [
          { isRight: true, content: '正确', analysis: '' },
          { isRight: false, content: '错误', analysis: '' }
        ]
      } else {
        sub.answerList = [
          { isRight: false, content: '', analysis: '' },
          { isRight: false, content: '', analysis: '' },
          { isRight: false, content: '', analysis: '' },
          { isRight: false, content: '', analysis: '' }
        ]
      }
    },

    addOption(sub) {
      sub.answerList.push({ isRight: false, content: '', analysis: '' })
    },

    removeOption(sub, index) {
      sub.answerList.splice(index, 1)
    },

    fetchData(id) {
      fetchDetail(id).then(response => {
        this.postForm = response.data
        if (!this.postForm.subQuList || this.postForm.subQuList.length === 0) {
          this.initSubList()
        }
      })
    },

    // 子题校验：返回错误信息或 null
    validateSub(sub, no) {
      if (!sub.content) {
        return no + '子题题干不能为空！'
      }
      if (!sub.score || sub.score <= 0) {
        return no + '子题分值必须大于0！'
      }
      if (!sub.answerList || sub.answerList.length < 2) {
        return no + '至少要有两个选项！'
      }
      let rightCount = 0
      for (const a of sub.answerList) {
        if (!a.content) {
          return no + '选项内容不能为空！'
        }
        if (a.isRight) {
          rightCount += 1
        }
      }
      if (rightCount < 1) {
        return no + '至少要有一个正确答案！'
      }
      if (sub.quType === 1 && rightCount !== 1) {
        return no + '单选题只能有一个正确答案！'
      }
      if (sub.quType === 2 && rightCount < 2) {
        return no + '多选题至少要有两个正确答案！'
      }
      if (sub.quType === 3 && rightCount !== 1) {
        return no + '判断题只能有一个正确项！'
      }
      return null
    },

    submitForm() {
      if (this.postForm.subQuList.length !== 5) {
        this.$message({ message: '综合题必须包含 5 个子题！', type: 'warning' })
        return
      }

      for (let i = 0; i < this.postForm.subQuList.length; i++) {
        const err = this.validateSub(this.postForm.subQuList[i], '第' + (i + 1) + '个子题：')
        if (err) {
          this.$message({ message: err, type: 'warning' })
          return
        }
      }

      this.$refs.postForm.validate((valid) => {
        if (!valid) {
          return
        }

        saveData(this.postForm).then(() => {
          this.$notify({
            title: '成功',
            message: '综合题保存成功！',
            type: 'success',
            duration: 2000
          })
          this.$router.push({ name: 'ListQu' })
        })
      })
    },

    onCancel() {
      this.$router.push({ name: 'ListQu' })
    }

  }
}
</script>

<style scoped>

</style>
