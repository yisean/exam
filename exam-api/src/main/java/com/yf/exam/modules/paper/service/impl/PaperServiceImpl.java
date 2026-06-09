package com.yf.exam.modules.paper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.ability.job.enums.JobGroup;
import com.yf.exam.ability.job.enums.JobPrefix;
import com.yf.exam.ability.job.service.JobService;
import com.yf.exam.core.api.ApiError;
import com.yf.exam.core.api.dto.PagingReqDTO;
import com.yf.exam.core.exception.ServiceException;
import com.yf.exam.core.utils.BeanMapper;
import com.yf.exam.core.utils.CronUtils;
import com.yf.exam.modules.exam.dto.ExamDTO;
import com.yf.exam.modules.exam.dto.ExamRepoDTO;
import com.yf.exam.modules.exam.dto.ExamTimeSlotDTO;
import com.yf.exam.modules.exam.dto.ext.ExamRepoExtDTO;
import com.yf.exam.modules.exam.entity.Exam;
import com.yf.exam.modules.exam.service.ExamEligibilityService;
import com.yf.exam.modules.exam.service.ExamRepoService;
import com.yf.exam.modules.exam.service.ExamService;
import com.yf.exam.modules.exam.service.ExamTimeSlotService;
import com.yf.exam.modules.user.UserUtils;
import com.yf.exam.modules.paper.dto.PaperDTO;
import com.yf.exam.modules.paper.dto.PaperQuDTO;
import com.yf.exam.modules.paper.dto.ext.PaperQuAnswerExtDTO;
import com.yf.exam.modules.paper.dto.ext.PaperQuDetailDTO;
import com.yf.exam.modules.paper.dto.request.PaperAnswerDTO;
import com.yf.exam.modules.paper.dto.request.PaperListReqDTO;
import com.yf.exam.modules.paper.dto.response.ExamDetailRespDTO;
import com.yf.exam.modules.paper.dto.response.ExamResultRespDTO;
import com.yf.exam.modules.paper.dto.response.PaperListRespDTO;
import com.yf.exam.modules.paper.entity.Paper;
import com.yf.exam.modules.paper.entity.PaperQu;
import com.yf.exam.modules.paper.entity.PaperQuAnswer;
import com.yf.exam.modules.paper.enums.ExamState;
import com.yf.exam.modules.paper.enums.PaperState;
import com.yf.exam.modules.paper.job.BreakExamJob;
import com.yf.exam.modules.paper.mapper.PaperMapper;
import com.yf.exam.modules.paper.service.PaperQuAnswerService;
import com.yf.exam.modules.paper.service.PaperQuService;
import com.yf.exam.modules.paper.service.PaperService;
import com.yf.exam.modules.qu.entity.Qu;
import com.yf.exam.modules.qu.entity.QuAnswer;
import com.yf.exam.modules.qu.enums.QuType;
import com.yf.exam.modules.qu.service.QuAnswerService;
import com.yf.exam.modules.qu.service.QuService;
import com.yf.exam.modules.sys.user.entity.SysUser;
import com.yf.exam.modules.sys.user.service.SysUserService;
import com.yf.exam.modules.user.book.service.UserBookService;
import com.yf.exam.modules.user.exam.service.UserExamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
* <p>
* 语言设置 服务实现类
* </p>
*
* @author 聪明笨狗
* @since 2020-05-25 16:33
*/
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {


    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ExamService examService;

    @Autowired
    private ExamTimeSlotService examTimeSlotService;

    @Autowired
    private ExamEligibilityService examEligibilityService;

    @Autowired
    private QuService quService;

    @Autowired
    private QuAnswerService quAnswerService;

    @Autowired
    private PaperService paperService;

    @Autowired
    private PaperQuService paperQuService;

    @Autowired
    private PaperQuAnswerService paperQuAnswerService;

    @Autowired
    private UserBookService userBookService;

    @Autowired
    private ExamRepoService examRepoService;

    @Autowired
    private UserExamService userExamService;

    @Autowired
    private JobService jobService;

    /**
     * 展示的选项，ABC这样
     */
    private static List<String> ABC = Arrays.asList(new String[]{
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K","L","M","N","O","P","Q","R","S","T","U","V","W","X"
            ,"Y","Z"
    });





    @Transactional(rollbackFor = Exception.class)
    @Override
    public String createPaper(String userId, String examId) {

        // 校验是否有正在考试的试卷
        QueryWrapper<Paper> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(Paper::getUserId, userId)
                .eq(Paper::getState, PaperState.ING);

        int exists = this.count(wrapper);


        if (exists > 0) {
            throw new ServiceException(ApiError.ERROR_20010002);
        }

        // 查找考试
        ExamDTO exam = examService.findById(examId);

        if(exam == null){
            throw new ServiceException(1, "考试不存在！");
        }

        if(!ExamState.ENABLE.equals(exam.getState())){
            throw new ServiceException(1, "考试状态不正确！");
        }

        // 时间段门禁：仅对配置了时间段的考试生效（历史考试沿用上面的状态校验）
        List<ExamTimeSlotDTO> slots = examTimeSlotService.listByExam(examId);
        if (!CollectionUtils.isEmpty(slots)) {
            Exam examEntity = examService.getById(examId);
            String departId = UserUtils.getDepartId(false);
            if (!examEligibilityService.canAnswer(examEntity, departId, new Date())) {
                throw new ServiceException(1, "当前不在该考试的可考时间段，或无应考资格！");
            }
        }

        // 考试题目列表
        List<PaperQu> quList = this.generateByRepo(examId);

        if(CollectionUtils.isEmpty(quList)){
            throw new ServiceException(1, "规则不正确，无对应的考题！");
        }

        //保存试卷内容
        Paper paper = this.savePaper(userId, exam, quList);

        // 强制交卷任务
        String jobName = JobPrefix.BREAK_EXAM + paper.getId();
        jobService.addCronJob(BreakExamJob.class, jobName, CronUtils.dateToCron(paper.getLimitTime()), paper.getId());

        return paper.getId();
    }

    @Override
    public ExamDetailRespDTO paperDetail(String paperId) {


        ExamDetailRespDTO respDTO = new ExamDetailRespDTO();

        // 试题基本信息
        Paper paper = paperService.getById(paperId);
        BeanMapper.copy(paper, respDTO);

        // 查找题目列表（按 sort 升序，父题在子题之前）
        List<PaperQuDTO> list = paperQuService.listByPaper(paperId);

        List<PaperQuDTO> radioList = new ArrayList<>();
        List<PaperQuDTO> multiList = new ArrayList<>();
        List<PaperQuDTO> judgeList = new ArrayList<>();
        List<PaperQuDTO> uncertainList = new ArrayList<>();
        List<PaperQuDTO> compositeList = new ArrayList<>();
        Map<String, PaperQuDTO> compositeMap = new HashMap<>(16);

        // 先收集综合题父题，建立映射
        for(PaperQuDTO item: list){
            if(QuType.COMPOSITE.equals(item.getQuType())){
                item.setSubList(new ArrayList<>());
                compositeList.add(item);
                compositeMap.put(item.getId(), item);
            }
        }

        // 再分配其余题目；综合题子题挂到父题 subList
        for(PaperQuDTO item: list){
            if(item.getParentId() != null && compositeMap.containsKey(item.getParentId())){
                compositeMap.get(item.getParentId()).getSubList().add(item);
            } else if(QuType.RADIO.equals(item.getQuType())){
                radioList.add(item);
            } else if(QuType.MULTI.equals(item.getQuType())){
                multiList.add(item);
            } else if(QuType.JUDGE.equals(item.getQuType())){
                judgeList.add(item);
            } else if(QuType.UNCERTAIN.equals(item.getQuType())){
                uncertainList.add(item);
            }
        }

        respDTO.setRadioList(radioList);
        respDTO.setMultiList(multiList);
        respDTO.setJudgeList(judgeList);
        respDTO.setUncertainList(uncertainList);
        respDTO.setCompositeList(compositeList);
        return respDTO;
    }

    @Override
    public ExamResultRespDTO paperResult(String paperId) {

        ExamResultRespDTO respDTO = new ExamResultRespDTO();

        // 试题基本信息
        Paper paper = paperService.getById(paperId);
        BeanMapper.copy(paper, respDTO);

        List<PaperQuDetailDTO> quList = paperQuService.listForPaperResult(paperId);
        respDTO.setQuList(quList);

        return respDTO;
    }

    @Override
    public PaperQuDetailDTO findQuDetail(String paperId, String quId) {

        PaperQuDetailDTO respDTO = new PaperQuDetailDTO();
        // 问题
        Qu qu = quService.getById(quId);

        // 基本信息
        PaperQu paperQu = paperQuService.findByKey(paperId, quId);
        BeanMapper.copy(paperQu, respDTO);
        respDTO.setContent(qu.getContent());
        respDTO.setImage(qu.getImage());

        // 答案列表
        List<PaperQuAnswerExtDTO> list = paperQuAnswerService.listForExam(paperId, quId);
        respDTO.setAnswerList(list);

        return respDTO;
    }


    /**
     * 题库组题方式产生题目列表
     * @param examId
     * @return
     */
    private List<PaperQu> generateByRepo(String examId){

        // 查找规则指定的题库
        List<ExamRepoExtDTO> list = examRepoService.listByExam(examId);

        //最终的题目列表
        List<PaperQu> quList = new ArrayList<>();

        //排除ID，避免题目重复
        List<String> excludes = new ArrayList<>();
        excludes.add("none");

        if (!CollectionUtils.isEmpty(list)) {
            for (ExamRepoExtDTO item : list) {

                // 单选题
                if(item.getRadioCount() > 0){
                    List<Qu> radioList = quService.listByRandom(item.getRepoId(), QuType.RADIO, excludes, item.getRadioCount());
                    for (Qu qu : radioList) {
                        PaperQu paperQu = this.processPaperQu(item, qu);
                        quList.add(paperQu);
                        excludes.add(qu.getId());
                    }
                }

                //多选题
                if(item.getMultiCount() > 0) {
                    List<Qu> multiList = quService.listByRandom(item.getRepoId(), QuType.MULTI, excludes,
                            item.getMultiCount());
                    for (Qu qu : multiList) {
                        PaperQu paperQu = this.processPaperQu(item, qu);
                        quList.add(paperQu);
                        excludes.add(qu.getId());
                    }
                }

                // 判断题
                if(item.getJudgeCount() > 0) {
                    List<Qu> judgeList = quService.listByRandom(item.getRepoId(), QuType.JUDGE, excludes,
                            item.getJudgeCount());
                    for (Qu qu : judgeList) {
                        PaperQu paperQu = this.processPaperQu(item, qu);
                        quList.add(paperQu);
                        excludes.add(qu.getId());
                    }
                }

                // 不定项题
                if(item.getUncertainCount() != null && item.getUncertainCount() > 0) {
                    List<Qu> uncertainList = quService.listByRandom(item.getRepoId(), QuType.UNCERTAIN, excludes,
                            item.getUncertainCount());
                    for (Qu qu : uncertainList) {
                        PaperQu paperQu = this.processPaperQu(item, qu);
                        quList.add(paperQu);
                        excludes.add(qu.getId());
                    }
                }

                // 综合题：整题抽取，拍平为「父行 + 5个子行」
                if(item.getCompositeCount() != null && item.getCompositeCount() > 0) {
                    List<Qu> compositeList = quService.listByRandom(item.getRepoId(), QuType.COMPOSITE, excludes,
                            item.getCompositeCount());
                    for (Qu parent : compositeList) {
                        excludes.add(parent.getId());

                        List<Qu> subs = quService.listByParent(parent.getId());
                        if (CollectionUtils.isEmpty(subs)) {
                            // 异常数据：综合题无子题，跳过
                            continue;
                        }

                        // 父行：预生成ID供子行引用；分值=子题之和；父题不参与判分
                        String parentPaperQuId = IdWorker.getIdStr();
                        int compositeScore = 0;
                        for (Qu sub : subs) {
                            compositeScore += (sub.getScore() == null ? 0 : sub.getScore());
                        }
                        PaperQu parentPq = new PaperQu();
                        parentPq.setId(parentPaperQuId);
                        parentPq.setQuId(parent.getId());
                        parentPq.setQuType(QuType.COMPOSITE);
                        parentPq.setAnswered(false);
                        parentPq.setIsRight(false);
                        parentPq.setScore(compositeScore);
                        parentPq.setActualScore(0);
                        quList.add(parentPq);

                        // 子行：各按自身题型判分，parentId 指向父行
                        for (Qu sub : subs) {
                            PaperQu subPq = new PaperQu();
                            subPq.setQuId(sub.getId());
                            subPq.setQuType(sub.getQuType());
                            subPq.setAnswered(false);
                            subPq.setIsRight(false);
                            subPq.setScore(sub.getScore() == null ? 0 : sub.getScore());
                            subPq.setActualScore(0);
                            subPq.setParentId(parentPaperQuId);
                            quList.add(subPq);
                        }
                    }
                }
            }
        }
        return quList;
    }



    /**
     * 填充试题题目信息
     * @param repo
     * @param qu
     * @return
     */
    private PaperQu processPaperQu(ExamRepoDTO repo, Qu qu) {

        //保存试题信息
        PaperQu paperQu = new PaperQu();
        paperQu.setQuId(qu.getId());
        paperQu.setAnswered(false);
        paperQu.setIsRight(false);
        paperQu.setQuType(qu.getQuType());
        // 实得分初始为0，交卷判分时按题型写回（支持不定项部分给分）
        paperQu.setActualScore(0);

        if (QuType.RADIO.equals(qu.getQuType())) {
            paperQu.setScore(repo.getRadioScore());
        }

        if (QuType.MULTI.equals(qu.getQuType())) {
            paperQu.setScore(repo.getMultiScore());
        }

        if (QuType.JUDGE.equals(qu.getQuType())) {
            paperQu.setScore(repo.getJudgeScore());
        }

        if (QuType.UNCERTAIN.equals(qu.getQuType())) {
            paperQu.setScore(repo.getUncertainScore());
        }

        return paperQu;
    }


    /**
     * 保存试卷
     * @param userId
     * @param exam
     * @param quList
     * @return
     */
    private Paper savePaper(String userId, ExamDTO exam, List<PaperQu> quList) {


        // 查找用户
        SysUser user = sysUserService.getById(userId);

        //保存试卷基本信息
        Paper paper = new Paper();
        paper.setDepartId(user.getDepartId());
        paper.setExamId(exam.getId());
        paper.setTitle(exam.getTitle());
        // 试卷实际总分 = 抽中的叶子题分值之和（综合题父行不计，其分由5个子行各自计入；
        // 综合题分值随题，无法在考试配置期精确预估，故以实际抽中题目为准）
        int totalScore = 0;
        if (!CollectionUtils.isEmpty(quList)) {
            for (PaperQu pq : quList) {
                if (!QuType.COMPOSITE.equals(pq.getQuType()) && pq.getScore() != null) {
                    totalScore += pq.getScore();
                }
            }
        }
        paper.setTotalScore(totalScore);
        paper.setTotalTime(exam.getTotalTime());
        paper.setUserScore(0);
        paper.setUserId(userId);
        paper.setCreateTime(new Date());
        paper.setUpdateTime(new Date());
        paper.setQualifyScore(exam.getQualifyScore());
        paper.setState(PaperState.ING);
        paper.setHasSaq(false);

        // 截止时间
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(System.currentTimeMillis());
        cl.add(Calendar.MINUTE, exam.getTotalTime());
        paper.setLimitTime(cl.getTime());

        paperService.save(paper);

        if (!CollectionUtils.isEmpty(quList)) {
            this.savePaperQu(paper.getId(), quList);
        }

        return paper;
    }



    /**
     * 保存试卷试题列表
     * @param paperId
     * @param quList
     */
    private void savePaperQu(String paperId, List<PaperQu> quList){

        List<PaperQu> batchQuList = new ArrayList<>();
        List<PaperQuAnswer> batchAnswerList = new ArrayList<>();

        int sort = 0;
        for (PaperQu item : quList) {

            item.setPaperId(paperId);
            item.setSort(sort);
            // 综合题父行已预生成ID供子行引用，不能覆盖
            if (StringUtils.isBlank(item.getId())) {
                item.setId(IdWorker.getIdStr());
            }

            //回答列表
            List<QuAnswer> answerList = quAnswerService.listAnswerByRandom(item.getQuId());

            if (!CollectionUtils.isEmpty(answerList)) {

                int ii = 0;
                for (QuAnswer answer : answerList) {
                    PaperQuAnswer paperQuAnswer = new PaperQuAnswer();
                    paperQuAnswer.setId(UUID.randomUUID().toString());
                    paperQuAnswer.setPaperId(paperId);
                    paperQuAnswer.setQuId(answer.getQuId());
                    paperQuAnswer.setAnswerId(answer.getId());
                    paperQuAnswer.setChecked(false);
                    paperQuAnswer.setSort(ii);
                    paperQuAnswer.setAbc(ABC.get(ii));
                    paperQuAnswer.setIsRight(answer.getIsRight());
                    ii++;
                    batchAnswerList.add(paperQuAnswer);
                }
            }

            batchQuList.add(item);
            sort++;
        }

        //添加问题
        paperQuService.saveBatch(batchQuList);

        //批量添加问题答案
        paperQuAnswerService.saveBatch(batchAnswerList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void fillAnswer(PaperAnswerDTO reqDTO) {


        // 本次是否有作答；取消全部选择(answers为空)时视为未作答，需把旧的勾选与得分清零，
        // 不能直接 return，否则会残留上一次的 actual_score/isRight。
        boolean answered = !(CollectionUtils.isEmpty(reqDTO.getAnswers())
                && StringUtils.isBlank(reqDTO.getAnswer()));

        //查找答案列表
        List<PaperQuAnswer> list = paperQuAnswerService.listForFill(reqDTO.getPaperId(), reqDTO.getQuId());

        // 本题（取题型与满分，用于计算实得分）
        PaperQu paperQu = paperQuService.findByKey(reqDTO.getPaperId(), reqDTO.getQuId());

        boolean hasWrongChecked = false;   // 选了错误项
        boolean allRightChecked = true;    // 所有正确项都被选中
        boolean anyRightChecked = false;   // 至少选中一个正确项

        //更新选项勾选状态
        for (PaperQuAnswer item : list) {

            boolean checked = reqDTO.getAnswers().contains(item.getId());
            item.setChecked(checked);

            boolean isRight = item.getIsRight() != null && item.getIsRight();
            if (isRight && checked) {
                anyRightChecked = true;
            }
            if (isRight && !checked) {
                allRightChecked = false;
            }
            if (!isRight && checked) {
                hasWrongChecked = true;
            }

            paperQuAnswerService.updateById(item);
        }

        Integer quType = paperQu != null ? paperQu.getQuType() : null;
        int score = (paperQu != null && paperQu.getScore() != null) ? paperQu.getScore() : 0;

        //修改为已回答 + 按题型判分写回实得分
        PaperQu qu = new PaperQu();
        qu.setQuId(reqDTO.getQuId());
        qu.setPaperId(reqDTO.getPaperId());
        qu.setAnswer(reqDTO.getAnswer());
        qu.setAnswered(answered);

        if (QuType.UNCERTAIN.equals(quType)) {
            // 不定项·固定半分制：错选0分；全对满分；漏选(无错选)得满分一半(向下取整)
            if (hasWrongChecked) {
                qu.setIsRight(false);
                qu.setActualScore(0);
            } else if (allRightChecked) {
                qu.setIsRight(true);
                qu.setActualScore(score);
            } else if (anyRightChecked) {
                qu.setIsRight(false);
                qu.setActualScore(score / 2);
            } else {
                qu.setIsRight(false);
                qu.setActualScore(0);
            }
        } else if (QuType.RADIO.equals(quType) || QuType.MULTI.equals(quType) || QuType.JUDGE.equals(quType)) {
            // 单选/多选/判断：全部选对得满分，否则0（与原行为一致）
            boolean right = !hasWrongChecked && allRightChecked;
            qu.setIsRight(right);
            qu.setActualScore(right ? score : 0);
        } else {
            // 主观题等：仅标记已答，不自动给分（由阅卷处理，actual_score 不在此更新）
            qu.setIsRight(true);
        }

        paperQuService.updateByKey(qu);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handExam(String paperId) {

        //获取试卷信息
        Paper paper = paperService.getById(paperId);

        //如果不是正常的，抛出异常
        if(!PaperState.ING.equals(paper.getState())){
            throw new ServiceException(1, "试卷状态不正确！");
        }

        // 客观分
        int objScore = paperQuService.sumObjective(paperId);
        paper.setObjScore(objScore);
        paper.setUserScore(objScore);

        // 主观分，因为要阅卷，所以给0
        paper.setSubjScore(0);

        // 待阅卷
        if(paper.getHasSaq()) {
            paper.setState(PaperState.WAIT_OPT);
        }else {

            // 同步保存考试成绩
            userExamService.joinResult(paper.getUserId(), paper.getExamId(), objScore, objScore>=paper.getQualifyScore());

            paper.setState(PaperState.FINISHED);
        }
        paper.setUpdateTime(new Date());

        //计算考试时长
        Calendar cl = Calendar.getInstance();
        cl.setTimeInMillis(System.currentTimeMillis());
        int userTime = (int)((System.currentTimeMillis() - paper.getCreateTime().getTime()) / 1000 / 60);
        if(userTime == 0){
            userTime = 1;
        }
        paper.setUserTime(userTime);

        //更新试卷
        paperService.updateById(paper);


        // 终止定时任务
        String name = JobPrefix.BREAK_EXAM + paperId;
        jobService.deleteJob(name, JobGroup.SYSTEM);

        //把打错的问题加入错题本
        List<PaperQuDTO> list = paperQuService.listByPaper(paperId);
        for(PaperQuDTO qu: list){
            // 综合题父题本身不入错题本（其子题各自按对错处理）
            if(QuType.COMPOSITE.equals(qu.getQuType())){
                continue;
            }
            // 主观题和对的都不加入错题库
            if(qu.getIsRight()){
                continue;
            }
            //加入错题本
            new Thread(() -> userBookService.addBook(paper.getExamId(), qu.getQuId())).run();
        }
    }

    @Override
    public IPage<PaperListRespDTO> paging(PagingReqDTO<PaperListReqDTO> reqDTO) {
        return baseMapper.paging(reqDTO.toPage(), reqDTO.getParams());
    }


    @Override
    public PaperDTO checkProcess(String userId) {

        QueryWrapper<Paper> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(Paper::getUserId, userId)
                .eq(Paper::getState, PaperState.ING);

        Paper paper = this.getOne(wrapper, false);

        if (paper != null) {
            return BeanMapper.map(paper, PaperDTO.class);
        }

        return null;
    }
}
