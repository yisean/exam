package com.yf.exam.modules.qu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.ability.upload.config.UploadConfig;
import com.yf.exam.core.api.dto.PagingReqDTO;
import com.yf.exam.core.exception.ServiceException;
import com.yf.exam.core.utils.BeanMapper;
import com.yf.exam.modules.qu.dto.QuAnswerDTO;
import com.yf.exam.modules.qu.dto.QuDTO;
import com.yf.exam.modules.qu.dto.export.QuExportDTO;
import com.yf.exam.modules.qu.dto.ext.QuDetailDTO;
import com.yf.exam.modules.qu.dto.request.QuQueryReqDTO;
import com.yf.exam.modules.qu.entity.Qu;
import com.yf.exam.modules.qu.entity.QuAnswer;
import com.yf.exam.modules.qu.entity.QuRepo;
import com.yf.exam.modules.qu.enums.QuType;
import com.yf.exam.modules.qu.mapper.QuMapper;
import com.yf.exam.modules.qu.service.QuAnswerService;
import com.yf.exam.modules.qu.service.QuRepoService;
import com.yf.exam.modules.qu.service.QuService;
import com.yf.exam.modules.qu.utils.ImageCheckUtils;
import com.yf.exam.modules.repo.service.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 语言设置 服务实现类
 * </p>
 *
 * @author 聪明笨狗
 * @since 2020-05-25 10:17
 */
@Service
public class QuServiceImpl extends ServiceImpl<QuMapper, Qu> implements QuService {

    @Autowired
    private QuAnswerService quAnswerService;

    @Autowired
    private QuRepoService quRepoService;

    @Autowired
    private ImageCheckUtils imageCheckUtils;

    @Override
    public IPage<QuDTO> paging(PagingReqDTO<QuQueryReqDTO> reqDTO) {

        //创建分页对象
        Page page = new Page<>(reqDTO.getCurrent(), reqDTO.getSize());

        //转换结果
        IPage<QuDTO> pageData = baseMapper.paging(page, reqDTO.getParams());
        return pageData;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<String> ids) {

        // 综合题：把其子题ID一并纳入删除集合（级联删子题）
        List<String> allIds = new ArrayList<>(ids);
        QueryWrapper<Qu> subWrapper = new QueryWrapper<>();
        subWrapper.lambda().in(Qu::getParentId, ids);
        List<Qu> subs = this.list(subWrapper);
        if (!CollectionUtils.isEmpty(subs)) {
            for (Qu s : subs) {
                allIds.add(s.getId());
            }
        }

        // 移除题目
        this.removeByIds(allIds);

        // 移除选项
        QueryWrapper<QuAnswer> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(QuAnswer::getQuId, allIds);
        quAnswerService.remove(wrapper);

        // 移除题库绑定
        QueryWrapper<QuRepo> wrapper1 = new QueryWrapper<>();
        wrapper1.lambda().in(QuRepo::getQuId, allIds);
        quRepoService.remove(wrapper1);
    }

    @Override
    public List<Qu> listByRandom(String repoId, Integer quType, List<String> excludes, Integer size) {
        return baseMapper.listByRandom(repoId, quType, excludes, size);
    }

    @Override
    public List<Qu> listByParent(String parentId) {
        QueryWrapper<Qu> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Qu::getParentId, parentId).orderByAsc(Qu::getSort);
        return this.list(wrapper);
    }

    @Override
    public QuDetailDTO detail(String id) {

        QuDetailDTO respDTO = new QuDetailDTO();
        Qu qu = this.getById(id);
        BeanMapper.copy(qu, respDTO);

        List<QuAnswerDTO> answerList = quAnswerService.listByQu(id);
        respDTO.setAnswerList(answerList);

        List<String> repoIds = quRepoService.listByQu(id);
        respDTO.setRepoIds(repoIds);

        // 综合题：回填5个子题（含各自选项与分值），按 sort 排序
        if (QuType.COMPOSITE.equals(qu.getQuType())) {
            QueryWrapper<Qu> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(Qu::getParentId, id).orderByAsc(Qu::getSort);
            List<Qu> subs = this.list(wrapper);
            List<QuDetailDTO> subList = new ArrayList<>();
            if (!CollectionUtils.isEmpty(subs)) {
                for (Qu sub : subs) {
                    QuDetailDTO subDTO = new QuDetailDTO();
                    BeanMapper.copy(sub, subDTO);
                    subDTO.setAnswerList(quAnswerService.listByQu(sub.getId()));
                    subList.add(subDTO);
                }
            }
            respDTO.setSubQuList(subList);
        }

        return respDTO;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void save(QuDetailDTO reqDTO) {

        // 综合题走父子保存
        if (QuType.COMPOSITE.equals(reqDTO.getQuType())) {
            this.saveComposite(reqDTO);
            return;
        }

        // 校验数据（单选/多选/判断/不定项）
        this.checkData(reqDTO, "");

        Qu qu = new Qu();
        BeanMapper.copy(reqDTO, qu);

        // 校验图片地址
        imageCheckUtils.checkImage(qu.getImage(), "题干图片地址错误！");

        // 更新
        this.saveOrUpdate(qu);

        // 保存全部问题
        quAnswerService.saveAll(qu.getId(), reqDTO.getAnswerList());

        // 保存到题库
        quRepoService.saveAll(qu.getId(), qu.getQuType(), reqDTO.getRepoIds());

    }

    /**
     * 保存综合题：父题（共享材料，无选项）+ 恰好5个子题（各含选项与分值）
     *
     * @param reqDTO
     */
    private void saveComposite(QuDetailDTO reqDTO) {

        // 校验综合题
        this.checkComposite(reqDTO);

        // 保存父题（综合题本身：content=共享材料，无父、无分值、无选项）
        Qu parent = new Qu();
        BeanMapper.copy(reqDTO, parent);
        parent.setParentId(null);
        parent.setSort(null);
        parent.setScore(null);
        imageCheckUtils.checkImage(parent.getImage(), "题干图片地址错误！");
        this.saveOrUpdate(parent);

        // 父题绑定题库（子题不单独绑库）
        quRepoService.saveAll(parent.getId(), QuType.COMPOSITE, reqDTO.getRepoIds());

        // 编辑场景：先清掉旧子题及其选项，再整体重建
        this.deleteSubQu(parent.getId());

        // 保存5个子题
        int sort = 0;
        for (QuDetailDTO sub : reqDTO.getSubQuList()) {
            Qu subQu = new Qu();
            BeanMapper.copy(sub, subQu);
            // 强制新增，建立父子关系与排序
            subQu.setId(null);
            subQu.setParentId(parent.getId());
            subQu.setSort(sort);
            imageCheckUtils.checkImage(subQu.getImage(), "子题图片地址错误！");
            this.saveOrUpdate(subQu);

            // 子题选项
            quAnswerService.saveAll(subQu.getId(), sub.getAnswerList());
            sort++;
        }
    }

    /**
     * 删除某综合题父题下的全部子题及其选项
     *
     * @param parentId
     */
    private void deleteSubQu(String parentId) {
        QueryWrapper<Qu> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Qu::getParentId, parentId);
        List<Qu> subs = this.list(wrapper);
        if (CollectionUtils.isEmpty(subs)) {
            return;
        }
        List<String> subIds = new ArrayList<>();
        for (Qu sub : subs) {
            subIds.add(sub.getId());
        }
        this.removeByIds(subIds);

        QueryWrapper<QuAnswer> aw = new QueryWrapper<>();
        aw.lambda().in(QuAnswer::getQuId, subIds);
        quAnswerService.remove(aw);
    }

    @Override
    public List<QuExportDTO> listForExport(QuQueryReqDTO query) {
        return baseMapper.listForExport(query);
    }

    @Override
    public int importExcel(List<QuExportDTO> dtoList) {

        //根据题目名称分组
        Map<Integer, List<QuExportDTO>> anMap = new HashMap<>(16);

        //题目本体信息
        Map<Integer, QuExportDTO> quMap = new HashMap<>(16);

        //数据分组
        for (QuExportDTO item : dtoList) {

            // 空白的ID
            if (StringUtils.isEmpty(item.getNo())) {
                continue;
            }

            Integer key;
            //序号
            try {
                key = Integer.parseInt(item.getNo());
            } catch (Exception e) {
                continue;
            }

            //如果已经有题目了，直接处理选项
            if (anMap.containsKey(key)) {
                anMap.get(key).add(item);
            } else {
                //如果没有，将题目内容和选项一起
                List<QuExportDTO> subList = new ArrayList<>();
                subList.add(item);
                anMap.put(key, subList);
                quMap.put(key, item);
            }
        }

        int count = 0;
        try {

            //循环题目插入
            for (Integer key : quMap.keySet()) {

                QuExportDTO im = quMap.get(key);

                //题目基本信息
                QuDetailDTO qu = new QuDetailDTO();
                qu.setContent(im.getQContent());
                qu.setAnalysis(im.getQAnalysis());
                qu.setQuType(Integer.parseInt(im.getQuType()));
                qu.setCreateTime(new Date());

                // 综合题含5个子题，Excel 无法承载，跳过以免整批导入中断
                if (QuType.COMPOSITE.equals(qu.getQuType())) {
                    continue;
                }

                //设置回答列表
                List<QuAnswerDTO> answerList = this.processAnswerList(anMap.get(key));
                //设置题目
                qu.setAnswerList(answerList);
                //设置引用题库
                qu.setRepoIds(im.getRepoList());
                // 保存答案
                this.save(qu);
                count++;
            }

        } catch (ServiceException e) {
            e.printStackTrace();
            throw new ServiceException(1, "导入出现问题，行：" + count + "，" + e.getMessage());
        }

        return count;
    }

    /**
     * 处理回答列表
     *
     * @param importList
     * @return
     */
    private List<QuAnswerDTO> processAnswerList(List<QuExportDTO> importList) {

        List<QuAnswerDTO> list = new ArrayList<>(16);
        for (QuExportDTO item : importList) {
            QuAnswerDTO a = new QuAnswerDTO();
            a.setIsRight("1".equals(item.getAIsRight()));
            a.setContent(item.getAContent());
            a.setAnalysis(item.getAAnalysis());
            a.setId("");
            list.add(a);
        }
        return list;
    }

    /**
     * 校验题目信息
     *
     * @param qu
     * @param no
     * @throws Exception
     */
    public void checkData(QuDetailDTO qu, String no) {

        if (StringUtils.isEmpty(qu.getContent())) {
            throw new ServiceException(1, no + "题目内容不能为空！");
        }

        if (CollectionUtils.isEmpty(qu.getRepoIds())) {
            throw new ServiceException(1, no + "至少要选择一个题库！");
        }

        this.checkAnswerList(qu.getAnswerList(), qu.getQuType(), no);
    }

    /**
     * 校验客观题选项（单选/多选/判断/不定项通用，也用于综合题子题）
     *
     * @param answers 选项列表
     * @param quType  题型
     * @param no      错误前缀
     */
    private void checkAnswerList(List<QuAnswerDTO> answers, Integer quType, String no) {

        if (CollectionUtils.isEmpty(answers)) {
            throw new ServiceException(1, no + "客观题至少要包含一个备选答案！");
        }

        int trueCount = 0;
        for (QuAnswerDTO a : answers) {

            if (a.getIsRight() == null) {
                throw new ServiceException(1, no + "必须定义选项是否正确项！");
            }

            if (StringUtils.isEmpty(a.getContent())) {
                throw new ServiceException(1, no + "选项内容不为空！");
            }

            if (a.getIsRight()) {
                trueCount += 1;
            }
        }

        if (trueCount == 0) {
            throw new ServiceException(1, no + "至少要包含一个正确项！");
        }

        //单选题
        if (QuType.RADIO.equals(quType) && trueCount > 1) {
            throw new ServiceException(1, no + "单选题不能包含多个正确项！");
        }

        //不定项：至少两个选项（答案可一个或多个）
        if (QuType.UNCERTAIN.equals(quType) && answers.size() < 2) {
            throw new ServiceException(1, no + "不定项至少要包含两个选项！");
        }
    }

    /**
     * 校验综合题：共享材料 + 题库 + 恰好5个子题（题型四选一、各带分值、各自选项合法）
     *
     * @param qu
     */
    private void checkComposite(QuDetailDTO qu) {

        if (StringUtils.isEmpty(qu.getContent())) {
            throw new ServiceException(1, "综合题的共享题干/材料不能为空！");
        }

        if (CollectionUtils.isEmpty(qu.getRepoIds())) {
            throw new ServiceException(1, "至少要选择一个题库！");
        }

        List<QuDetailDTO> subs = qu.getSubQuList();
        if (subs == null || subs.size() != 5) {
            throw new ServiceException(1, "综合题必须包含5个子题！");
        }

        int i = 1;
        for (QuDetailDTO sub : subs) {

            String no = "第" + i + "个子题：";
            Integer type = sub.getQuType();

            if (type == null
                    || !(QuType.RADIO.equals(type) || QuType.MULTI.equals(type)
                    || QuType.JUDGE.equals(type) || QuType.UNCERTAIN.equals(type))) {
                throw new ServiceException(1, no + "子题题型只能是单选/多选/判断/不定项（不能是综合题）！");
            }

            if (sub.getScore() == null || sub.getScore() <= 0) {
                throw new ServiceException(1, no + "子题分值必须大于0！");
            }

            if (StringUtils.isEmpty(sub.getContent())) {
                throw new ServiceException(1, no + "子题题干不能为空！");
            }

            this.checkAnswerList(sub.getAnswerList(), type, no);
            i++;
        }
    }
}
