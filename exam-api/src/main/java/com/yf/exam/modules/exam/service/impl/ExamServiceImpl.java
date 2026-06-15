package com.yf.exam.modules.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.core.api.dto.PagingReqDTO;
import com.yf.exam.core.enums.OpenType;
import com.yf.exam.core.exception.ServiceException;
import com.yf.exam.core.utils.BeanMapper;
import com.yf.exam.modules.exam.dto.ExamDTO;
import com.yf.exam.modules.exam.dto.ExamRepoDTO;
import com.yf.exam.modules.exam.dto.ExamTimeSlotDTO;
import com.yf.exam.modules.exam.dto.ext.ExamRepoExtDTO;
import com.yf.exam.modules.exam.dto.request.ExamSaveReqDTO;
import com.yf.exam.modules.exam.dto.response.ExamOnlineRespDTO;
import com.yf.exam.modules.exam.dto.response.ExamReviewRespDTO;
import com.yf.exam.modules.exam.entity.Exam;
import com.yf.exam.modules.exam.mapper.ExamMapper;
import com.yf.exam.modules.exam.service.ExamDepartService;
import com.yf.exam.modules.exam.service.ExamEligibilityService;
import com.yf.exam.modules.exam.service.ExamRepoService;
import com.yf.exam.modules.exam.service.ExamService;
import com.yf.exam.modules.exam.service.ExamTimeSlotService;
import com.yf.exam.modules.paper.enums.ExamState;
import com.yf.exam.modules.user.UserUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
* <p>
* 考试业务实现类
* </p>
*
* @author 聪明笨狗
* @since 2020-07-25 16:18
*/
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {


    @Autowired
    private ExamRepoService examRepoService;

    @Autowired
    private ExamDepartService examDepartService;

    @Autowired
    private ExamTimeSlotService examTimeSlotService;

    @Autowired
    private ExamEligibilityService examEligibilityService;

    @Override
    public void save(ExamSaveReqDTO reqDTO) {

        // ID
        String id = reqDTO.getId();

        if(StringUtils.isBlank(id)){
            id = IdWorker.getIdStr();
        }

        //复制参数
        Exam entity = new Exam();

        // 计算分值
        this.calcScore(reqDTO);


        // 复制基本数据
        BeanMapper.copy(reqDTO, entity);
        entity.setId(id);

        // 修复状态
        if (reqDTO.getTimeLimit()!=null
                && !reqDTO.getTimeLimit()
                && reqDTO.getState()!=null
                && reqDTO.getState() == 2) {
            entity.setState(0);
        } else {
            entity.setState(reqDTO.getState());
        }

        // 题库组卷
        try {
            examRepoService.saveAll(id, reqDTO.getRepoList());
        }catch (DuplicateKeyException e){
            throw new ServiceException(1, "不能选择重复的题库！");
        }


        // 开放的部门
        if(OpenType.DEPT_OPEN.equals(reqDTO.getOpenType())){
            examDepartService.saveAll(id, reqDTO.getDepartIds());
        }

        // 时间段（含段-部门），全量重写；无时间段时清空
        examTimeSlotService.saveAll(id, reqDTO.getTimeSlots());

        this.saveOrUpdate(entity);

    }

    @Override
    public ExamSaveReqDTO findDetail(String id) {
        ExamSaveReqDTO respDTO = new ExamSaveReqDTO();
        Exam exam = this.getById(id);
        BeanMapper.copy(exam, respDTO);

        // 考试部门
        List<String> departIds = examDepartService.listByExam(id);
        respDTO.setDepartIds(departIds);

        // 题库
        List<ExamRepoExtDTO> repos = examRepoService.listByExam(id);
        respDTO.setRepoList(repos);

        // 时间段（含段-部门）
        respDTO.setTimeSlots(examTimeSlotService.listByExam(id));

        return respDTO;
    }

    @Override
    public ExamDTO findById(String id) {
        ExamDTO respDTO = new ExamDTO();
        Exam exam = this.getById(id);
        BeanMapper.copy(exam, respDTO);
        return respDTO;
    }

    @Override
    public IPage<ExamDTO> paging(PagingReqDTO<ExamDTO> reqDTO) {

        //创建分页对象
        Page page = new Page(reqDTO.getCurrent(), reqDTO.getSize());

        //转换结果
        IPage<ExamDTO> pageData = baseMapper.paging(page, reqDTO.getParams());
        return pageData;
     }

    @Override
    public IPage<ExamOnlineRespDTO> onlinePaging(PagingReqDTO<ExamDTO> reqDTO) {

        String departId = UserUtils.getDepartId(false);
        Date now = new Date();

        // 查询启用的考试（state=0），可选标题过滤
        QueryWrapper<Exam> wrapper = new QueryWrapper<>();
        wrapper.eq("state", ExamState.ENABLE);
        ExamDTO params = reqDTO.getParams();
        if (params != null && StringUtils.isNotBlank(params.getTitle())) {
            wrapper.like("title", params.getTitle());
        }
        wrapper.orderByDesc("create_time");
        List<Exam> exams = this.list(wrapper);

        // 按时间段资格过滤并组装（无时间段的考试沿用历史开放规则）
        List<ExamOnlineRespDTO> all = new ArrayList<>();
        for (Exam exam : exams) {
            if (!examEligibilityService.isVisible(exam, departId, now)) {
                continue;
            }
            ExamOnlineRespDTO dto = new ExamOnlineRespDTO();
            BeanMapper.copy(exam, dto);
            List<ExamTimeSlotDTO> slots = examEligibilityService.listVisibleSlots(exam.getId(), departId, now);
            dto.setTimeSlots(slots);
            dto.setCanAnswer(examEligibilityService.canAnswer(exam, departId, now));
            all.add(dto);
        }

        // 手工分页（企业季度考核数据量小）
        long current = reqDTO.getCurrent();
        long size = reqDTO.getSize();
        int from = (int) Math.max(0, (current - 1) * size);
        int to = (int) Math.min(all.size(), from + size);
        List<ExamOnlineRespDTO> records = from >= all.size() ? new ArrayList<>() : new ArrayList<>(all.subList(from, to));

        Page<ExamOnlineRespDTO> page = new Page<>(current, size);
        page.setTotal(all.size());
        page.setRecords(records);
        return page;
    }

    @Override
    public IPage<ExamReviewRespDTO> reviewPaging(PagingReqDTO<ExamDTO> reqDTO) {
        // 创建分页对象
        Page page = new Page(reqDTO.getCurrent(), reqDTO.getSize());

        // 查找分页
        IPage<ExamReviewRespDTO> pageData = baseMapper.reviewPaging(page, reqDTO.getParams());

        return pageData;
    }


    /**
     * 计算分值
     * @param reqDTO
     */
    private void calcScore(ExamSaveReqDTO reqDTO){

        // 主观题分数
        int objScore = 0;

        // 题库组卷
        List<ExamRepoExtDTO> repoList = reqDTO.getRepoList();

        for(ExamRepoDTO item: repoList){
            if(item.getRadioCount()!=null
                    && item.getRadioCount()>0
                    && item.getRadioScore()!=null
                    && item.getRadioScore()>0){
                objScore+=item.getRadioCount()*item.getRadioScore();
            }
            if(item.getMultiCount()!=null
                    && item.getMultiCount()>0
                    && item.getMultiScore()!=null
                    && item.getMultiScore()>0){
                objScore+=item.getMultiCount()*item.getMultiScore();
            }
            if(item.getJudgeCount()!=null
                    && item.getJudgeCount()>0
                    && item.getJudgeScore()!=null
                    && item.getJudgeScore()>0){
                objScore+=item.getJudgeCount()*item.getJudgeScore();
            }
            if(item.getUncertainCount()!=null
                    && item.getUncertainCount()>0
                    && item.getUncertainScore()!=null
                    && item.getUncertainScore()>0){
                objScore+=item.getUncertainCount()*item.getUncertainScore();
            }
            // 简答题：数量×每题分值确定（同单选/多选口径），计入总分；阅卷在此满分内打分。
            // 同时做配对校验：有数量必须有分值、有分值必须有数量（与前端组卷校验、createPaper 一致）。
            if (item.getSaqCount() != null && item.getSaqCount() > 0) {
                if (item.getSaqScore() == null || item.getSaqScore() <= 0) {
                    throw new ServiceException(1, "存在简答题数量但未配置每题分值！");
                }
                objScore += item.getSaqCount() * item.getSaqScore();
            } else if (item.getSaqScore() != null && item.getSaqScore() > 0) {
                throw new ServiceException(1, "存在简答题分值但未配置题目数量！");
            }

            // 综合题分值随题（=子题分值之和），抽取随机，配置期无法精确预估，
            // 不计入此处估算；试卷实际总分在交卷判分时以抽中题目为准（见 PaperServiceImpl.savePaper）。
        }



        reqDTO.setTotalScore(objScore);
    }

}
