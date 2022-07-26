package cn.bctools.document.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.bctools.common.exception.BusinessException;
import cn.bctools.common.utils.ObjectNull;
import cn.bctools.common.utils.TenantContextHolder;
import cn.bctools.common.utils.function.Get;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.document.component.UserComponent;
import cn.bctools.document.constant.IndexConstant;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.entity.DcLibraryUser;
import cn.bctools.document.entity.enums.DcLibraryReadEnum;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.mapper.DcLibraryMapper;
import cn.bctools.document.mapper.DcLibraryUserMapper;
import cn.bctools.document.po.DocumentEsPo;
import cn.bctools.document.po.DocumentLogEsPo;
import cn.bctools.document.po.enums.DocumentLogTypeEnum;
import cn.bctools.document.service.DocumentElasticService;
import cn.bctools.document.util.DcEsUtil;
import cn.bctools.document.util.HtmlUtil;
import cn.bctools.document.vo.req.DocumentRecentlyUpdatedVo;
import cn.bctools.document.vo.req.DocumentSearchVo;
import cn.bctools.document.vo.res.DocumentEditLogResVo;
import cn.bctools.document.vo.res.DocumentRecentlyUpdatedResVo;
import cn.bctools.document.vo.res.DocumentSearchResVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-文档es服务
 */
@Slf4j
@Service
@AllArgsConstructor
public class DocumentElasticServiceImpl implements DocumentElasticService {

    private ElasticsearchRestTemplate esTemplate;
    private DcLibraryUserMapper libraryUserMapper;
    private DcLibraryMapper libraryMapper;


    @Override
    public Page<DocumentSearchResVo> searchDoc(Page page, DocumentSearchVo documentSearchVo, String userId) {
        try {
            Page<DocumentSearchResVo> resVoPage = new Page<>(page.getCurrent(), page.getSize());
            String tenantId = TenantContextHolder.getTenantId();
            // 1. 获取查询的知识库类型，用以限定搜索范围。 目前只支持查询知识库|目录下的文档
            // 搜索知识库范围（未指定知识库时为空， 可指定知识库 | 目录）
            DcLibraryTypeEnum dcType = null;
            if (StringUtils.isNotBlank(documentSearchVo.getKnowledgeId())) {
                DcLibrary dcLibrary = libraryMapper.selectOne(Wrappers.<DcLibrary>lambdaQuery().eq(DcLibrary::getId, documentSearchVo.getKnowledgeId()).select(DcLibrary::getType));
                if (dcLibrary == null) {
                    return resVoPage;
                }
                dcType = dcLibrary.getType();
                if (!DcLibraryTypeEnum.knowledge.equals(dcType) && !DcLibraryTypeEnum.directory.equals(dcType)) {
                    return resVoPage;
                }
            }

            // 2. 构造查询条件
            // 获取有权限的知识库id集合
            Set<String> knowledgeIds = new HashSet<>();
            // 有知识库权限的租户id集合
            Set<String> tenantIds = new HashSet<>();
            buildSearchDocCondition(tenantId, knowledgeIds, tenantIds, documentSearchVo, userId, dcType);
            if (CollectionUtils.isEmpty(knowledgeIds)) {
                return resVoPage;
            }

            // 3. 搜索
            SearchPage<DocumentEsPo> searchPage = searchDocEsQuery(page, documentSearchVo, dcType, knowledgeIds, tenantIds);

            // 4. 构造返回
            List<DocumentSearchResVo> resDatas = new ArrayList<>();
            long total = Optional.ofNullable(searchPage.getContent().size()).orElse(0) == 0 ? 0 : searchPage.getTotalElements();
            resVoPage = new Page<>(page.getCurrent(), page.getSize(), total);
            for (SearchHit<DocumentEsPo> documentEsPoSearchHit : searchPage.getContent()) {
                DocumentSearchResVo resVo = new DocumentSearchResVo();
                BeanUtils.copyProperties(documentEsPoSearchHit.getContent(), resVo);
                // 若有高亮数据，则返回高亮数据
                List<String> highNames = documentEsPoSearchHit.getHighlightFields().get(Get.name(DocumentEsPo::getName));
                List<String> highContents = documentEsPoSearchHit.getHighlightFields().get(Get.name(DocumentEsPo::getContent));
                resVo.setName(CollectionUtils.isEmpty(highNames) ? resVo.getName() : highNames.get(0));
                resVo.setContent(CollectionUtils.isEmpty(highContents) ? resVo.getContent() : highContents.get(0));
                resDatas.add(resVo);
            }
            resVoPage.setRecords(resDatas);
            return resVoPage;
        } catch (Exception e) {
            log.error("ES搜索失败. exception: {}", e);
            throw new BusinessException("查询文档失败");
        }
    }

    /**
     * 搜索 —— 构造搜索条件
     *
     * @param tenantId         租户id
     * @param knowledgeIds     有权限的知识库id集合
     * @param tenantIds        有权限的知识库id集合对应的租户id集合
     * @param documentSearchVo 搜索入参
     * @param userId           登录用户id（未登录时没有）
     * @param dcType           搜索知识库范围（未指定知识库时为空， 可指定知识库 | 目录）
     */
    private void buildSearchDocCondition(String tenantId, Set<String> knowledgeIds, Set<String> tenantIds, DocumentSearchVo documentSearchVo, String userId, DcLibraryTypeEnum dcType) {
        // 搜索范围：未登录-搜索所有租户完全开放的知识库；已登录-搜索所有租户完全开放的知识库 + 用户是成员的知识库 + 用户所在租户开放的知识库
        // 获取完全开放的知识库(不限租户)
        TenantContextHolder.clear();
        List<DcLibrary> dcLibraries = libraryMapper.selectList(Wrappers.<DcLibrary>lambdaQuery().eq(DcLibrary::getShareRole, DcLibraryReadEnum.all).select(DcLibrary::getId, DcLibrary::getTenantId));
        if (CollectionUtils.isNotEmpty(dcLibraries)) {
            for (DcLibrary dcLibrary : dcLibraries) {
                knowledgeIds.add(dcLibrary.getId());
                tenantIds.add(dcLibrary.getTenantId());
            }
        }

        TenantContextHolder.setTenantId(tenantId);
        // 登录用户知识库搜索
        if (StringUtils.isNotBlank(userId)) {
            // 已登录，从用户有权限的知识库中搜索
            LambdaQueryWrapper<DcLibraryUser> query = Wrappers.<DcLibraryUser>lambdaQuery()
                    .eq(DcLibraryUser::getUserId, userId)
                    .eq(documentSearchVo.getKnowledgeId() != null && dcType != null && dcType == DcLibraryTypeEnum.knowledge, DcLibraryUser::getDcLibraryId, documentSearchVo.getKnowledgeId())
                    .select(DcLibraryUser::getDcLibraryId);
            List<DcLibraryUser> dcLibraryUsers = libraryUserMapper.selectList(query);
            if (CollectionUtils.isNotEmpty(dcLibraryUsers)) {
                knowledgeIds.addAll(dcLibraryUsers.stream().map(DcLibraryUser::getDcLibraryId).collect(Collectors.toSet()));
                Optional.ofNullable(tenantId).map(tenantIds::add);
            }

            // 获取当前租户下，开放给所有用户的知识库
            List<DcLibrary> registerDcLibrary = libraryMapper.selectList(Wrappers.<DcLibrary>lambdaQuery()
                    .eq(DcLibrary::getShareRole, DcLibraryReadEnum.register)
                    .eq(DcLibrary::getType, DcLibraryTypeEnum.knowledge)
                    .select(DcLibrary::getId, DcLibrary::getTenantId));
            if (CollectionUtils.isNotEmpty(registerDcLibrary)) {
                knowledgeIds.addAll(registerDcLibrary.stream().map(DcLibrary::getId).collect(Collectors.toSet()));
                Optional.ofNullable(tenantId).map(tenantIds::add);
            }
        }

        // 指定知识库搜索
        if (DcLibraryTypeEnum.knowledge == dcType) {
            // 无权限，则直接返回空
            if (!knowledgeIds.contains(documentSearchVo.getKnowledgeId())) {
                knowledgeIds.clear();
                return;
            }
            // 清空查询条件，并设置当前知识库id和租户id作为查询条件
            knowledgeIds.clear();
            tenantIds.clear();
            knowledgeIds.add(documentSearchVo.getKnowledgeId());
            Optional.ofNullable(tenantId).map(tenantIds::add);
        }
    }

    /**
     * ES搜索文档
     *
     * @param page
     * @param documentSearchVo
     * @param dcType
     * @param knowledgeIds
     * @return
     */
    private SearchPage<DocumentEsPo> searchDocEsQuery(Page page, DocumentSearchVo documentSearchVo, DcLibraryTypeEnum dcType, Set<String> knowledgeIds, Set<String> tenantIds) {
        // 构造搜索条件
        // 指定知识库查询， 若无权限，直接返回，若有权限，则只查询指定知识库
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        boolQuery.filter(QueryBuilders.termsQuery(Get.name(DocumentEsPo::getKnowledgeId), knowledgeIds));
        if (CollectionUtils.isNotEmpty(tenantIds)) {
            boolQuery.filter(QueryBuilders.termsQuery(Get.name(DocumentEsPo::getTenantId), tenantIds));
        }
        if (DcLibraryTypeEnum.directory == dcType) {
            boolQuery.filter(QueryBuilders.termQuery(Get.name(DocumentEsPo::getParentId), documentSearchVo.getKnowledgeId()));
        }
        if (StringUtils.isNotBlank(documentSearchVo.getKeyword())) {
            BoolQueryBuilder keyWorkQuery = new BoolQueryBuilder();
            keyWorkQuery.should(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getName), documentSearchVo.getKeyword()));
            keyWorkQuery.should(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getContent), documentSearchVo.getKeyword()));
            boolQuery.must(keyWorkQuery);
        }
        // 搜索排除类型为“知识库”和“目录”的数据
        boolQuery.mustNot(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getType), DcLibraryTypeEnum.knowledge));
        boolQuery.mustNot(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getType), DcLibraryTypeEnum.directory));

        // 默认按匹配度降序排序
        NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of((int) (page.getCurrent() - 1), (int) page.getSize()))
                .withHighlightFields(new HighlightBuilder.Field(Get.name(DocumentEsPo::getName)), new HighlightBuilder.Field(Get.name(DocumentEsPo::getContent)))
                .build();

        // 执行搜索
        SearchHits<DocumentEsPo> searchHits = esTemplate.search(queryBuilder, DocumentEsPo.class);
        return SearchHitSupport.searchPageFor(searchHits, queryBuilder.getPageable());
    }

    @Override
    public void save(UserDto userDto, DcLibrary dcLibrary, String content) {
        try {
            String conditionId = DcLibraryTypeEnum.knowledge.equals(dcLibrary.getType()) ? dcLibrary.getId() : dcLibrary.getKnowledgeId();
            DcLibrary knowledge = libraryMapper.selectById(conditionId);
            DocumentEsPo esPo = build(userDto, dcLibrary, content, knowledge.getName());
            esTemplate.save(esPo);
        } catch (Exception e) {
            log.error("ES保存知识库文档失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }
    }

    /**
     * 封装documentEsPO 入库信息
     *
     * @param userDto       用户
     * @param dcLibrary     知识库文档信息
     * @param content       文档内容
     * @param knowledgeName 知识库名称
     * @return
     */
    @Override
    public DocumentEsPo build(UserDto userDto, DcLibrary dcLibrary, String content, String knowledgeName) {
        DocumentEsPo documentEsPo = new DocumentEsPo();
        documentEsPo.setId(buildDocumentEsId(dcLibrary.getTenantId(), dcLibrary.getId()));
        documentEsPo.setDocId(dcLibrary.getId());
        documentEsPo.setType(dcLibrary.getType());
        documentEsPo.setTenantId(dcLibrary.getTenantId());
        documentEsPo.setName(dcLibrary.getName());
        // 存储纯文本内容(不包括html标签，不是JSON字符串)
        if (DcLibraryTypeEnum.document_html.equals(dcLibrary.getType())) {
            documentEsPo.setContent(HtmlUtil.replaceHtmlTag(JSONUtil.isJson(content) ? JSONUtil.parseObj(content).getStr(Get.name(DcLibrary::getContent)) : content, " "));
        }
        documentEsPo.setKnowledgeName(knowledgeName);
        // 类型为knowledge的数据没有knowledgeId，就使用当前知识库id填充
        documentEsPo.setKnowledgeId(StringUtils.isEmpty(dcLibrary.getKnowledgeId()) ? dcLibrary.getId() : dcLibrary.getKnowledgeId());
        documentEsPo.setCreateTime(dcLibrary.getCreateTime());
        documentEsPo.setParentId(dcLibrary.getParentId());
        documentEsPo.setAuthorId(userDto.getId());
        documentEsPo.setAuthorName(userDto.getRealName());
        return documentEsPo;
    }

    /**
     * DocumentEs 索引id
     *
     * @param tenantId 租户id
     * @param dcId     知识库id
     * @return
     */
    private String buildDocumentEsId(String tenantId, String dcId) {
        return DcEsUtil.buildEsId(tenantId, dcId);
    }


    @Async
    @Override
    public void saveLog(DcLibrary dcLibrary, String realName, String userId, DocumentLogTypeEnum logTypeEnum, LocalDateTime time) {
        // 知识库id为空，表示当前操作的是知识库
        String dcId = dcLibrary.getKnowledgeId() == null ? dcLibrary.getId() : dcLibrary.getKnowledgeId();
        DcLibrary knowledge = libraryMapper.selectById(dcId);
        DocumentLogEsPo documentLogEsPo = new DocumentLogEsPo();
        documentLogEsPo.setId(DcEsUtil.buildEsId(TenantContextHolder.getTenantId(), dcLibrary.getId()) + "_" + time.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        documentLogEsPo.setDocId(dcLibrary.getId());
        documentLogEsPo.setType(dcLibrary.getType());
        documentLogEsPo.setLogType(logTypeEnum);
        documentLogEsPo.setTenantId(TenantContextHolder.getTenantId());
        documentLogEsPo.setName(dcLibrary.getName());
        documentLogEsPo.setUserId(userId);
        documentLogEsPo.setCreateTime(time);
        documentLogEsPo.setUserName(realName);
        documentLogEsPo.setKnowledgeName(knowledge.getName());
        documentLogEsPo.setKnowledgeId(dcLibrary.getKnowledgeId());
        esTemplate.save(documentLogEsPo);
    }

    @Override
    public Page<DocumentEditLogResVo> searchDocumentEditLog(Page page, String id) {
        try {
            // 构造查询条件
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.filter(QueryBuilders.matchQuery(Get.name(DocumentLogEsPo::getDocId), id));
            boolQuery.must(QueryBuilders.matchQuery(Get.name(DocumentLogEsPo::getLogType), DocumentLogTypeEnum.EDIT));

            NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .withSort(SortBuilders.fieldSort(Get.name(DocumentLogEsPo::getCreateTime)).order(SortOrder.DESC))
                    .withPageable(PageRequest.of((int) (page.getCurrent() - 1), (int) page.getSize()))
                    .build();

            // 执行搜索
            SearchHits<DocumentLogEsPo> searchHits = esTemplate.search(queryBuilder, DocumentLogEsPo.class);
            SearchPage<DocumentLogEsPo> searchPage = SearchHitSupport.searchPageFor(searchHits, queryBuilder.getPageable());

            // 构造返回
            List<DocumentEditLogResVo> resDatas = new ArrayList<>();
            long total = CollectionUtils.isEmpty(searchPage.getContent()) ? 0 : searchPage.getTotalElements();
            Page<DocumentEditLogResVo> resVoPage = new Page<>(page.getCurrent(), page.getSize(), total);
            for (SearchHit<DocumentLogEsPo> documentLogEsPoSearchHit : searchPage.getContent()) {
                DocumentEditLogResVo resVo = new DocumentEditLogResVo();
                BeanUtils.copyProperties(documentLogEsPoSearchHit.getContent(), resVo);
                resDatas.add(resVo);
            }
            resVoPage.setRecords(resDatas);

            return resVoPage;
        } catch (Exception e) {
            log.error("查询文档编辑记录失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }

    }

    @Override
    public Long searchDocumentReadTotal(String id) {
        try {
            // 构造查询条件
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.filter(QueryBuilders.matchQuery(Get.name(DocumentLogEsPo::getDocId), id));
            boolQuery.must(QueryBuilders.matchQuery(Get.name(DocumentLogEsPo::getLogType), DocumentLogTypeEnum.READ));
            NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder().withQuery(boolQuery).build();

            return esTemplate.count(queryBuilder, DocumentLogEsPo.class);
        } catch (Exception e) {
            log.error("获取文档已读次数失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }

    }


    @Override
    public List<DocumentRecentlyUpdatedResVo> searchDocumentRecentlyUpdate(DocumentRecentlyUpdatedVo recentlyVo, List<DcLibrary> dcLibraries) {
        try {
            List<DocumentRecentlyUpdatedResVo> resDatas = new ArrayList<>();

            Set<String> docIds = dcLibraries.stream().map(DcLibrary::getId).collect(Collectors.toSet());

            // 2. 构造查询条件
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.filter(QueryBuilders.termsQuery(Get.name(DocumentLogEsPo::getDocId), docIds));
            boolQuery.must(QueryBuilders.matchQuery(Get.name(DocumentLogEsPo::getLogType), DocumentLogTypeEnum.EDIT));

            // 构造各个文档最近更新记录聚合条件
            final String termsAggs = "group_by_docId";
            final String recentlyAggs = "recently";
            TermsAggregationBuilder aggsGroupBuilder = AggregationBuilders.terms(termsAggs).field(Get.name(DocumentLogEsPo::getDocId));
            TopHitsAggregationBuilder aggsRecentlyBuilder = AggregationBuilders.topHits(recentlyAggs).sort(SortBuilders.fieldSort(Get.name(DocumentLogEsPo::getCreateTime)).order(SortOrder.DESC)).size(1);

            NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .addAggregation(aggsGroupBuilder.subAggregation(aggsRecentlyBuilder))
                    .withPageable(PageRequest.of(0, 1))
                    .build();

            // 执行搜索
            SearchHits<DocumentLogEsPo> searchHits = esTemplate.search(queryBuilder, DocumentLogEsPo.class);

            // 解析聚合桶
            Aggregations entitiesAggregations = searchHits.getAggregations();
            if (entitiesAggregations == null) {
                return Collections.emptyList();
            }
            Terms terms = (Terms) entitiesAggregations.asMap().get(termsAggs);
            for (Terms.Bucket bucket : terms.getBuckets()) {
                TopHits topScoreResult = bucket.getAggregations().get(recentlyAggs);
                if (ObjectNull.isNotNull(topScoreResult) && topScoreResult.getHits().getHits().length > 0) {
                    org.elasticsearch.search.SearchHit hit = topScoreResult.getHits().getAt(0);
                    DocumentRecentlyUpdatedResVo documentEditLogResVo = JSON.parseObject(hit.getSourceAsString(), DocumentRecentlyUpdatedResVo.class);
                    resDatas.add(documentEditLogResVo);
                }
            }

            // 结果集按时间倒序，返回指定数量内的数据
            return resDatas.stream().sorted(Comparator.comparing(DocumentRecentlyUpdatedResVo::getCreateTime).reversed())
                    .limit(recentlyVo.getSize())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询指定知识库最近更新文档集合失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }

    }

    @Override
    public List<DocumentEsPo> searchDocumentByName(String name) {
        try {
            List<DocumentEsPo> resDatas = new ArrayList<>();
            NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getName), name)).build();
            SearchHits<DocumentEsPo> searchHits = esTemplate.search(queryBuilder, DocumentEsPo.class);
            for (SearchHit<DocumentEsPo> documentEsPoSearchHit : searchHits.getSearchHits()) {
                DocumentEsPo resVo = new DocumentEsPo();
                BeanUtils.copyProperties(documentEsPoSearchHit.getContent(), resVo);
                resDatas.add(resVo);
            }
            return resDatas;
        } catch (Exception e) {
            log.error("根据文档名称，搜索文档信息集合失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }

    }

    @Override
    public void deleteDocument(String tenantId, String docId) {
        try {
            if (docId == null) {
                log.warn("文档id为空，不能删除文档");
                return;
            }
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.must(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getTenantId), tenantId));
            boolQuery.must(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getDocId), docId));
            NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder().withQuery(boolQuery).build();
            esTemplate.delete(queryBuilder, DocumentEsPo.class, IndexConstant.INDEX_DOCUMENT_BASE_INFO);
        } catch (Exception e) {
            log.error("删除知识库失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }
    }

    @Override
    public void updateDocumentEs(DcLibrary dcLibrary) {
        try {
            String id = buildDocumentEsId(dcLibrary.getTenantId(), dcLibrary.getId());
            Document document = Document.create();
            document.put(Get.name(DocumentEsPo::getName), dcLibrary.getName());
            UpdateQuery updateQuery = UpdateQuery.builder(id).withDocument(document).build();
            esTemplate.update(updateQuery, IndexConstant.INDEX_DOCUMENT_BASE_INFO);

            // 若知识库名称变更，则更新该知识库关联的所有es索引文档中的“知识库名称”
            updateKnowledgeName(dcLibrary);
        } catch (Exception e) {
            log.error("修改文档信息失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }
    }

    /**
     * 若知识库名称变更，则更新该知识库关联的所有es索引文档中的“知识库名称”
     *
     * @param dcLibrary
     */
    private void updateKnowledgeName(DcLibrary dcLibrary) {
        if (!DcLibraryTypeEnum.knowledge.equals(dcLibrary.getType())) {
            return;
        }
        try {
            // 需要同步修改知识库名称的条件
            List<UpdateQuery> updateQuerys = new ArrayList<>();

            // 查询需要修改知识库名称的索引
            BoolQueryBuilder boolQuery = new BoolQueryBuilder();
            boolQuery.must(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getKnowledgeId), dcLibrary.getId()));
            boolQuery.must(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getTenantId), dcLibrary.getTenantId()));
            boolQuery.mustNot(QueryBuilders.matchPhraseQuery(Get.name(DocumentEsPo::getKnowledgeName), dcLibrary.getName()));
            NativeSearchQuery queryBuilder = new NativeSearchQueryBuilder().withQuery(boolQuery).build();
            SearchHits<DocumentEsPo> searchHits = esTemplate.search(queryBuilder, DocumentEsPo.class);
            for (SearchHit<DocumentEsPo> documentEsPoSearchHit : searchHits.getSearchHits()) {
                DocumentEsPo resVo = new DocumentEsPo();
                BeanUtils.copyProperties(documentEsPoSearchHit.getContent(), resVo);
                String id = buildDocumentEsId(resVo.getTenantId(), resVo.getDocId());
                Document document = Document.create();
                document.put(Get.name(DocumentEsPo::getKnowledgeName), dcLibrary.getName());
                UpdateQuery updateQuery = UpdateQuery.builder(id).withDocument(document).build();
                updateQuerys.add(updateQuery);
            }
            if (CollectionUtils.isEmpty(updateQuerys)) {
                return;
            }

            // 批量修改知识库名称
            esTemplate.bulkUpdate(updateQuerys, IndexConstant.INDEX_DOCUMENT_BASE_INFO);
        } catch (Exception e) {
            log.error("批量更新知识库名称失败. exception: {}", e);
            throw new BusinessException("ES异常");
        }
    }
}
