package cn.bctools.document.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableSet;
import cn.bctools.common.exception.BusinessException;
import cn.bctools.common.utils.R;
import cn.bctools.common.utils.TenantContextHolder;
import cn.bctools.common.utils.function.Get;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.document.component.RoleComponent;
import cn.bctools.document.component.ShareComponent;
import cn.bctools.document.component.UserComponent;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.entity.DcLibraryUser;
import cn.bctools.document.entity.enums.DcLibraryReadEnum;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.enums.OperationEnum;
import cn.bctools.document.po.DocumentEsPo;
import cn.bctools.document.po.enums.DocumentLogTypeEnum;
import cn.bctools.document.service.DcLibraryService;
import cn.bctools.document.service.DcLibraryUserService;
import cn.bctools.document.service.DocumentElasticService;
import cn.bctools.document.utils.FileOssUtils;
import cn.bctools.document.vo.req.*;
import cn.bctools.document.vo.res.*;
import cn.bctools.log.annotation.Log;
import cn.bctools.oauth2.utils.UserCurrentUtils;

import cn.bctools.oss.dto.BaseFile;
import cn.bctools.oss.template.OssTemplate;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库操作
 *
 * @author guojing
 */
@Slf4j
@Api(tags = "知识库")
@RestController
@RequestMapping(value = "/dcLibrary")
@AllArgsConstructor
public class DcLibraryController {

    DcLibraryService dcLibraryService;
    DcLibraryUserService dcLibraryUserService;
    DocumentElasticService documentElasticService;
    UserComponent userComponent;
    OssTemplate ossTemplate;
    RoleComponent roleComponent;
    ShareComponent shareComponent;

    @Log
    @ApiOperation("添加知识库、目录、文件")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public R<DcLibrary> add(@RequestBody @Validated DcLibraryAddReqVo reqVo) {
        UserDto currentUser = UserCurrentUtils.getCurrentUser();
        DcLibrary dcLibrary = dcLibraryService.add(currentUser, reqVo);
        // 日志入库ES
        documentElasticService.saveLog(dcLibrary, currentUser.getRealName(), currentUser.getId(), DocumentLogTypeEnum.CREATE, LocalDateTime.now());
        dcLibrary.setTenantId(TenantContextHolder.getTenantId());
        // 保存到文档es
        documentElasticService.save(currentUser, dcLibrary, "");
        return R.ok(dcLibrary);
    }

    /**
     * 重命名 知识库 目录 文档
     * 修改设置
     */
    @Log
    @PutMapping
    @ApiOperation("重命名知识库/目录/文档，或设置知识库")
    public R<DcLibrary> put(@RequestBody @Validated DcLibrary dto) {
        UserDto currentUser = UserCurrentUtils.getCurrentUser();
        DcLibrary dcLibrary = dcLibraryService.put(currentUser, dto);
        // 日志入库ES
        documentElasticService.saveLog(dcLibrary, currentUser.getRealName(), currentUser.getId(), DocumentLogTypeEnum.EDIT, LocalDateTime.now());
        // 修改文档es
        documentElasticService.updateDocumentEs(dcLibrary);
        return R.ok(dcLibrary);
    }

    @Log
    @DeleteMapping("/{id}")
    @ApiOperation("删除")
    public R<String> delete(@PathVariable String id) {
        // 权限校验
        roleComponent.checkOperationAuthority(OperationEnum.DC_LIBRARY_DEL, UserCurrentUtils.getUserId(), id);
        dcLibraryService.delete(UserCurrentUtils.getUserId(), id);
        return R.ok();
    }

    @Log
    @GetMapping("/tree")
    @ApiOperation("获取目录树")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "知识库id")
    })
    public R<List<DcLibrary>> tree(String id, ShareCheckReqVo req) {
        String userId = UserCurrentUtils.getUserId();
        return R.ok(dcLibraryService.tree(userId, id, req));
    }

    @Log
    @GetMapping("/page")
    @ApiOperation("文档分页查询")
    public R<Page<DcLibrary>> page(Page<DcLibrary> page, DcLibrary dto) {
        //获取用户所关联的知识库的所有文档
        Set<String> knowledgeIds = dcLibraryUserService.list(Wrappers.<DcLibraryUser>lambdaQuery().eq(DcLibraryUser::getUserId, UserCurrentUtils.getUserId()).select(DcLibraryUser::getDcLibraryId)).stream().map(DcLibraryUser::getDcLibraryId).collect(Collectors.toSet());
        if (ObjectUtil.isEmpty(knowledgeIds)) {
            return R.ok(page);
        }
        List<String> subdirectory = new ArrayList<>();
        if (ObjectUtil.isNotNull(dto.getId())) {
            //获得儿子
            subdirectory = dcLibraryService.subdirectory(dto.getId());
            //添加自己
            subdirectory.add(dto.getId());
            //去重
            subdirectory = subdirectory.stream().distinct().collect(Collectors.toList());
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        List<String> list = new ArrayList<>();
        if (StringUtils.isNotBlank(dto.getName())) {
            // 添加基本分词查询
            queryBuilder.withQuery(QueryBuilders.matchQuery(Get.name(DocumentEsPo::getName), dto.getName()).minimumShouldMatch("1"));
            // 搜索，获取结果
            list = documentElasticService.searchDocumentByName(dto.getName()).stream().map(DocumentEsPo::getDocId).collect(Collectors.toList());
        }
        dcLibraryService.page(page, Wrappers.<DcLibrary>lambdaQuery()
                //where 1=1 and (name like concat('%','','%') or name in ('aa','bb'))
                //根据ES查询条件
                .like(StrUtil.isNotBlank(dto.getName()), DcLibrary::getName, StrUtil.trim(dto.getName()))
                .or().in(!list.isEmpty(), DcLibrary::getId, list)
                .in(ObjectUtil.isNotEmpty(subdirectory), DcLibrary::getParentId, subdirectory)
                //按文档类型
                .eq(ObjectUtil.isNotNull(dto.getType()), DcLibrary::getType, dto.getType())
                //不包含目录和知识库
                .notIn(DcLibrary::getType, DcLibraryTypeEnum.directory, DcLibraryTypeEnum.knowledge)
                //都只能看到自己关联知识库下面的文档
                .in(DcLibrary::getKnowledgeId, knowledgeIds)
                //只查必要的
                .select(DcLibrary::getId, DcLibrary::getName, DcLibrary::getCreateBy, DcLibrary::getUpdateTime, DcLibrary::getSize, DcLibrary::getType)
                //先按创建时间降序 然后排序升序
                .orderByAsc(DcLibrary::getOrderId).orderByDesc(DcLibrary::getCreateTime));
        return R.ok(page);
    }


    @SneakyThrows
    @Log
    @PostMapping("/save/content/{documentId}")
    @ApiOperation("保存文档")
    public R<String> saveContent(@RequestBody String content, @PathVariable String documentId) {

        DcLibrary dcLibrary = dcLibraryService.getById(documentId);
        //上传文档
        String originalName = FileOssUtils.getMultipartFileName(dcLibrary, content);

        //根据知识库创建目录
        DcLibrary know = dcLibraryService.getById(dcLibrary.getKnowledgeId());
        //
        BaseFile baseFile = ossTemplate.putContent(originalName, content, know.getName(), dcLibrary.getName());

        dcLibrary.setBucketName(baseFile.getBucketName());
        dcLibrary.setFilePath(baseFile.getFileName());
        UserDto currentUser = UserCurrentUtils.getCurrentUser();

        dcLibraryService.saveContent(currentUser.getId(), dcLibrary, documentId);

        //同步ES
        try {
            // 日志入库ES
            documentElasticService.saveLog(dcLibrary, currentUser.getRealName(), currentUser.getId(), DocumentLogTypeEnum.EDIT, LocalDateTime.now());
            // 保存到文档es
            documentElasticService.save(currentUser, dcLibrary, content);
        } catch (Exception e) {
            log.error("同步ES错误：{}", e.getMessage());
            throw new BusinessException("保存失败");
        }

        return R.ok("保存成功");
    }

    @SneakyThrows
    @Log
    @ApiOperation("上传文档")
    @PostMapping("/upload/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "parentId", value = "上级目录id", required = true)
    })
    public R<String> upload(@PathVariable("id") String id, @RequestParam("file") MultipartFile file) {
        UserDto currentUser = UserCurrentUtils.getCurrentUser();
        String originalFilename = file.getOriginalFilename();
        //查询点击的当前元素
        DcLibrary dcLibrary = dcLibraryService.getById(id);

        //根据知识库创建目录
//        DcLibrary know = dcLibraryService.getById(dcLibrary.getKnowledgeId());

        BaseFile baseFile = ossTemplate.putFile(file.getOriginalFilename(), file.getInputStream(), dcLibrary.getName(), dcLibrary.getName());
        dcLibrary.setFilePath(baseFile.getFileName());
        dcLibrary.setBucketName(baseFile.getBucketName());
        dcLibraryService.updateById(dcLibrary);
        String fileLink = ossTemplate.fileLink(baseFile.getFileName(), baseFile.getBucketName());

        // 日志入库ES
//        documentElasticService.saveLog(dcLibrary, currentUser.getRealName(), currentUser.getId(), DocumentLogTypeEnum.CREATE, LocalDateTime.now());
        // 保存到文档es
//        documentElasticService.save(currentUser, dcLibrary, "");

//        dcLibraryService.uploadDocument(currentUser.getId(), parentId, originalFilename, dcLibrary);
        return R.ok(fileLink);
    }


    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Log
    @GetMapping("/preview/document/{id}")
    @ApiOperation("预览文档")
    public R<PreviewDocumentResVo> preview(@PathVariable("id") String id, ShareCheckReqVo shareReqVo) {
        DcLibrary library = dcLibraryService.getById(id);
        if (library == null) {
            return R.failed("文档不存在");
        }

        String userId = UserCurrentUtils.getUserId();
        // 查询权限校验
        checkReadRole(shareReqVo, userId, library);

        //直接更 更新没有正在编辑的为正在编辑中
        boolean edit = dcLibraryService.update(library, Wrappers.<DcLibrary>lambdaUpdate().eq(DcLibrary::getId, library.getId()).isNull(DcLibrary::getEditingBy).set(DcLibrary::getEditingBy, userId));
        //更新成功
        if (edit) {
            library.setEditingBy(userId);
        }

        PreviewDocumentResVo resVo = new PreviewDocumentResVo();
        //只读还是可以编辑 当前判断
        DcLibrary library2 = dcLibraryService.getById(id);
        resVo.setType(ObjectUtil.equal(library2.getEditingBy(), userId) ? "edit" : "read");
        resVo.setCreateTime(library2.getCreateTime());
        //内容
        if (StringUtils.isNotBlank(library.getFilePath())) {
            String objectURL = ossTemplate.fileLink(library.getFilePath(), library.getBucketName());
            if (library.getType().equals(DcLibraryTypeEnum.document_unrecognized)) {
                resVo.setContent(objectURL);
            } else {
                byte[] bytes = HttpUtil.downloadBytes(objectURL);
                resVo.setContent(bytes == null ? null : ObjectUtil.deserialize(bytes));
            }
        }

        // 封装文档作者
        List<String> authorIds = Arrays.asList(library2.getCreateById());
        Map<String, UserDto> userMap = userComponent.getUserMap(authorIds);
        resVo.setAuthor(userMap.get(library2.getCreateById()).

                getRealName());

        // 日志入库ES
        if (StringUtils.isNotBlank(userId)) {
            documentElasticService.saveLog(library, UserCurrentUtils.getCurrentUser().getRealName(), UserCurrentUtils.getUserId(), DocumentLogTypeEnum.READ, LocalDateTime.now());
        }

        return R.ok(resVo);
    }

    @Log
    @PutMapping("/status/document/{id}/{action}")
    @ApiOperation("更新文档为编辑状态")
    public R<String> status(@PathVariable("id") String id,
                            @ApiParam("true则为编辑锁定状态,false为解锁编辑状态") @PathVariable("action") Boolean action) {
        String userId = UserCurrentUtils.getUserId();
        DcLibrary library = dcLibraryService.getById(id);
        if (Boolean.TRUE.equals(action)) {
            //在getEditingBy为null的基础上改
            dcLibraryService.update(library, Wrappers.<DcLibrary>lambdaUpdate().eq(DcLibrary::getId, library.getId()).isNull(DcLibrary::getEditingBy).set(DcLibrary::getEditingBy, userId));
        } else {
            //在getEditingBy为userId的基础上改
            dcLibraryService.update(library, Wrappers.<DcLibrary>lambdaUpdate().eq(DcLibrary::getId, library.getId()).eq(DcLibrary::getEditingBy, userId).set(DcLibrary::getEditingBy, null));
        }
        return R.ok();
    }

    @Log
    @ApiOperation("搜索文档")
    @GetMapping("/document/search")
    public R<Page<DocumentSearchResVo>> searchDoc(Page page, DocumentSearchVo documentSearchVo) {
        String userId = UserCurrentUtils.getUserId();
        return R.ok(documentElasticService.searchDoc(page, documentSearchVo, userId));
    }

    @Log
    @ApiOperation("查询用户有权限的知识库")
    @GetMapping("/knowledges")
    public R<Page<DcLibrary>> findKnowledge(Page page) {
        return R.ok(dcLibraryService.queryKnowledge(page, UserCurrentUtils.getUserId()));
    }

    @Log
    @ApiOperation("查询用户自己创建的知识库")
    @GetMapping("/knowledges/owner")
    public R<Page<DcLibrary>> findOwnerKnowledge(Page page) {
        return R.ok(dcLibraryService.queryOwnerKnowledge(page, UserCurrentUtils.getUserId()));
    }

    @Log
    @ApiOperation("查询文档编辑记录")
    @GetMapping("/document/edit/log")
    public R<Page<DocumentEditLogResVo>> searchDocumentEditLog(Page page, @Validated DocumentEditLogVo documentEditLogVo) {
        return R.ok(documentElasticService.searchDocumentEditLog(page, documentEditLogVo.getId()));
    }

    @Log
    @ApiOperation("获取文档已读次数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "文档id", required = true)
    })
    @GetMapping("/document/read/total/{id}")
    public R<Long> searchDocumentReadTotal(@PathVariable String id) {
        return R.ok(documentElasticService.searchDocumentReadTotal(id));
    }


    @Log
    @ApiOperation("查询指定知识库最近更新文档")
    @GetMapping("/document/recently/updated")
    public R<List<DocumentRecentlyUpdatedResVo>> searchDocumentRecentlyUpdate(@Validated DocumentRecentlyUpdatedVo recentlyVo) {

        // 1. 根据知识库id 或 目录id，获取所有下级节点中的文件id
        Set<String> dcIds = dcLibraryService.getAllChildDcLibraryId(recentlyVo.getId());
        if (CollectionUtils.isEmpty(dcIds)) {
            return R.ok();
        }
        List<DcLibrary> dcLibraries = dcLibraryService.getDocumentByIds(dcIds);
        if (CollectionUtils.isEmpty(dcLibraries)) {
            return R.ok();
        }

        List<DocumentRecentlyUpdatedResVo> resVos = documentElasticService.searchDocumentRecentlyUpdate(recentlyVo, dcLibraries);
        // 获取头像
        List<String> userIds = resVos.stream().map(DocumentRecentlyUpdatedResVo::getUserId).collect(Collectors.toList());
        Map<String, UserDto> userMap = userComponent.getUserMap(userIds);
        resVos.stream().forEach(e -> e.setHeadImg(userMap.get(e.getUserId()).getHeadImg()));
        return R.ok(resVos);
    }

    @Log
    @ApiOperation("查询指定知识库目录下(包含所有下级节点)文件总数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "目录id", required = true)
    })
    @GetMapping("/document/total/{id}")
    public R<Integer> searchLibraryDocumentTotal(@PathVariable String id) {
        Integer count = 0;
        Set<String> dcIds = dcLibraryService.getAllChildDcLibraryId(id);
        if (CollectionUtils.isEmpty(dcIds)) {
            return R.ok(count);
        }
        List<DcLibrary> dcLibraries = dcLibraryService.getDocumentByIds(dcIds);
        if (CollectionUtils.isEmpty(dcLibraries)) {
            return R.ok(count);
        }
        return R.ok(dcLibraries.size());
    }

    @Log
    @ApiOperation("查询指定目录下(不含子目录)所有文件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "目录id", required = true)
    })
    @GetMapping("/document/list/{id}")
    public R<List<DocumentResVo>> searchLibraryDocuments(@PathVariable String id) {
        List<DcLibrary> dcLibraries = dcLibraryService.getDocumentByIds(ImmutableSet.of(id));
        if (CollectionUtils.isEmpty(dcLibraries)) {
            return R.ok(Collections.emptyList());
        }
        List<DocumentResVo> documentResVos = null;
        // 封装文档作者
        List<String> authorIds = dcLibraries.stream().map(DcLibrary::getCreateById).collect(Collectors.toList());
        Map<String, UserDto> userMap = userComponent.getUserMap(authorIds);
        documentResVos = dcLibraries.stream().map(e -> {
            DocumentResVo resVo = BeanUtil.copyProperties(e, DocumentResVo.class);
            resVo.setAuthor(userMap.get(e.getCreateById()).getRealName());
            return resVo;
        }).collect(Collectors.toList());
        return R.ok(documentResVos);
    }

    @Log
    @ApiOperation(value = "获取知识库信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id（知识库id、目录id、文档id）", value = "id", required = true)
    })
    @GetMapping("/info/{id}")
    public R<DcLibrary> queryDcLibraryInfo(@PathVariable String id, ShareCheckReqVo shareReqVo) {
        // 查询
        DcLibrary dcLibraryInfo = dcLibraryService.getById(id);
        if (dcLibraryInfo == null) {
            throw new BusinessException("资源不存在");
        }
        // 编辑次数越多，path版本数据量越大。只返回最新版本path
//        JSONArray filePaths = JSONUtil.parseArray(StrUtil.blankToDefault(dcLibraryInfo.getFilePath(), "[]"));
//        String currentPath = CollectionUtils.isEmpty(filePaths) ? null : JSONUtil.toJsonStr(filePaths.get(filePaths.size() - 1));
//        dcLibraryInfo.setFilePath(currentPath);
        // 查询权限校验
        checkReadRole(shareReqVo, UserCurrentUtils.getUserId(), dcLibraryInfo);
        return R.ok(dcLibraryInfo);
    }

    /**
     * 校验知识库查看权限
     *
     * @param shareReqVo 分享请求
     * @param userId     登录用户id
     * @param dcLibrary  知识库
     */
    private void checkReadRole(ShareCheckReqVo shareReqVo, String userId, DcLibrary dcLibrary) {
        if (StringUtils.isNotBlank(shareReqVo.getKey())) {
            shareComponent.checkShare(shareReqVo);
        } else {
            if (!DcLibraryTypeEnum.knowledge.equals(dcLibrary.getType())) {
                dcLibrary = dcLibraryService.getKnowledgeByChildren(dcLibrary.getId());
            }
            if (dcLibrary == null) {
                throw new BusinessException("该节点的知识库不存在");
            }
            //未登录，只能查看完全开放的知识库
            if (StringUtils.isBlank(userId) && !DcLibraryReadEnum.all.equals(dcLibrary.getShareRole())) {
                throw new BusinessException("无权访问");
            } else {
                // 已登录，知识库不是完全开放的，并且不是文档库成员，并且文档库不是注册用户开放的，则不能查询
                boolean registerRole = DcLibraryReadEnum.register.equals(dcLibrary.getShareRole()) && dcLibrary.getTenantId().equals(TenantContextHolder.getTenantId());
                if (!DcLibraryReadEnum.all.equals(dcLibrary.getShareRole()) && !registerRole && dcLibraryUserService.count(Wrappers.<DcLibraryUser>lambdaQuery().eq(DcLibraryUser::getDcLibraryId, dcLibrary.getId()).eq(DcLibraryUser::getUserId, userId)) <= 0) {
                    throw new BusinessException("无权访问");
                }
            }
        }
    }

    @Log
    @ApiOperation(value = "设置文档查看提醒")
    @PutMapping("/notify/read/setting")
    public R<String> settingReadNotify(@RequestBody @Validated SettingReadNotifyReqVo reqVo) {
        // 权限校验
        roleComponent.checkOperationAuthority(OperationEnum.READ_NOTIFY_SETTING, UserCurrentUtils.getUserId(), reqVo.getId());

        // 数据校验
        DcLibrary dcLibrary = dcLibraryService.getById(reqVo.getId());
        if (dcLibrary == null) {
            throw new BusinessException("知识库文档不存在");
        }
        if (DcLibraryTypeEnum.knowledge.equals(dcLibrary.getType()) || DcLibraryTypeEnum.directory.equals(dcLibrary.getType())) {
            throw new BusinessException("只支持文档设置查看提醒开关");
        }

        // 保存设置
        dcLibrary.setReadNotify(reqVo.getReadNotify());
        dcLibraryService.updateById(dcLibrary);
        return R.ok();
    }
}
