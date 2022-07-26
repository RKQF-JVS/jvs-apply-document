package cn.bctools.document.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.bctools.common.exception.BusinessException;
import cn.bctools.common.utils.ObjectNull;
import cn.bctools.common.utils.TenantContextHolder;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.document.component.RoleComponent;
import cn.bctools.document.component.ShareComponent;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.entity.DcLibraryUser;
import cn.bctools.document.entity.enums.*;
import cn.bctools.document.enums.OperationEnum;
import cn.bctools.document.mapper.DcLibraryMapper;
import cn.bctools.document.mapper.DcLibraryUserMapper;
import cn.bctools.document.service.DcLibraryService;
import cn.bctools.document.service.DocumentElasticService;
import cn.bctools.document.service.NotifyService;
import cn.bctools.document.util.DcLibraryUtil;
import cn.bctools.document.vo.req.DcLibraryAddReqVo;
import cn.bctools.document.vo.req.ShareCheckReqVo;
import cn.bctools.document.vo.req.ShareSaveReqVo;
import cn.bctools.document.vo.res.ShareCheckResVo;
import cn.bctools.oss.template.OssTemplate;
import cn.bctools.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author auto
 */
@Slf4j
@Service
public class DcLibraryServiceImpl extends ServiceImpl<DcLibraryMapper, DcLibrary> implements DcLibraryService {

    @Autowired
    DcLibraryUserMapper dcLibraryUserMapper;
    @Autowired
    DocumentElasticService documentElasticService;
    @Autowired
    RoleComponent roleComponent;
    @Autowired
    ShareComponent shareComponent;
    @Autowired
    NotifyService notifyService;
    @Autowired
    OssTemplate ossTemplate;

    @Autowired
    RedisUtils redisUtils;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeDc(String id) {
        DcLibrary byId = getById(id);
        if (ObjectUtil.isEmpty(byId)) {
            //递归退出
            return;
        }
        if (byId.getType().equals(DcLibraryTypeEnum.directory) || byId.getType().equals(DcLibraryTypeEnum.knowledge)) {
            //删除知识库或目录
            this.removeById(id);
            if (byId.getType().equals(DcLibraryTypeEnum.knowledge)) {
                //若为知识库 删除成员
                dcLibraryUserMapper.delete(Wrappers.<DcLibraryUser>lambdaUpdate().eq(DcLibraryUser::getDcLibraryId, id));
            }
            //判断有没有下级，如果有下级，递归删除
            List<String> list = this.list(Wrappers.<DcLibrary>lambdaQuery().eq(DcLibrary::getParentId, id).select(DcLibrary::getId)).stream().map(DcLibrary::getId).collect(Collectors.toList());
            if (ObjectUtil.isNotEmpty(list)) {
                list.forEach(this::removeDc);
            }
        } else {
            //删除文档
            this.removeById(id);
        }
        // 删除文档所在es数据
        documentElasticService.deleteDocument(byId.getTenantId(), byId.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DcLibrary add(UserDto userDto, DcLibraryAddReqVo reqVo) {
        DcLibrary dcLibrary = new DcLibrary();
        dcLibrary.setName(reqVo.getName());
        dcLibrary.setDescription(reqVo.getDescription());
        dcLibrary.setColor(reqVo.getColor());
        dcLibrary.setReadNotify(reqVo.getReadNotify());

        //新增 知识库（任何人都可以创建知识库）
        if (reqVo.getId() == null) {
            //如果id 不传 则为新增 知识库
            //只需要名称和类型
            dcLibrary.setType(DcLibraryTypeEnum.knowledge);
            dcLibrary.setShareRole(reqVo.getShareRole());
            //如果是知识库,创建所属人
            save(dcLibrary);
            DcLibraryUser dcLibraryUser = new DcLibraryUser();
            dcLibraryUser.setUserId(userDto.getId());
            dcLibraryUser.setRealName(userDto.getRealName());
            dcLibraryUser.setRole(DcLibraryUserRoleEnum.owner);
            dcLibraryUser.setDcLibraryId(dcLibrary.getId());
            dcLibraryUserMapper.insert(dcLibraryUser);
            return dcLibrary;
        }
        //查询点击的当前元素
        DcLibrary byId = getById(reqVo.getId());
        if (byId == null) {
            throw new BusinessException("目录不存在");
        }
        // 权限校验
        roleComponent.checkOperationAuthority(OperationEnum.DC_LIBRARY_ADD, userDto.getId(), byId.getId());

        DcLibraryTypeEnum type = byId.getType();
        switch (type) {
            case knowledge:
                // 只能新增目录
                // 子集 目录
                dcLibrary.setType(DcLibraryTypeEnum.directory).setParentId(byId.getId());
            case directory:
                // 为空默认给目录
                DcLibraryTypeEnum fileType = Optional.ofNullable(reqVo.getFileType()).orElse(DcLibraryTypeEnum.directory);
                // 如果为传递的知识库变为目录
                fileType = ObjectUtil.equal(fileType, DcLibraryTypeEnum.knowledge) ? DcLibraryTypeEnum.directory : fileType;
                if (fileType == DcLibraryTypeEnum.directory) {
                    // 子集 目录
                    dcLibrary.setType(DcLibraryTypeEnum.directory).setParentId(byId.getId());
                } else {
                    // 新建各种文档
                    dcLibrary.setType(fileType).setParentId(byId.getId());
                }
                break;
            default:
                log.warn("未知的知识库类型");
                break;
        }
        //设置知识库id
        dcLibrary.setKnowledgeId(Optional.ofNullable(this.getKnowledgeByChildren(reqVo.getId())).orElseThrow(() -> new BusinessException("该" + dcLibrary.getType().desc + "对应的知识库不存在")).getId());
        //新增目录或文档
        save(dcLibrary);
        return dcLibrary;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public DcLibrary settingShare(ShareSaveReqVo reqVo) {
        // 校验
        if (null == ShareSettingTypeEnum.getByValue(reqVo.getSettingType().value)) {
            throw new BusinessException("无效的分享设置类型");
        }

        // 获取分享信息
        DcLibrary dcLibrary = getById(reqVo.getId());
        if (dcLibrary == null) {
            throw new BusinessException("分享失败,资源不存在");
        }
        String oldKey = dcLibrary.getShareKey();

        // 设置分享链接
        if (ShareSettingTypeEnum.CREATE_URL.equals(reqVo.getSettingType())) {
            settingShareCreateUrl(reqVo, dcLibrary);
        }

        // 设置密码
        if (ShareSettingTypeEnum.PWD.equals(reqVo.getSettingType())) {
            settingSharePwd(reqVo, dcLibrary);
        }

        // 设置时效
        dcLibrary.setShareValidityType(ShareSettingTypeEnum.VALIDITY.equals(reqVo.getSettingType()) ? reqVo.getValidityType() : dcLibrary.getShareValidityType());
        // 设置时效，计算过期时间
        if (ShareSettingTypeEnum.VALIDITY.equals(reqVo.getSettingType())) {
            settingShareValidity(reqVo, dcLibrary);
        }

        // 停止分享
        boolean closeShare = reqVo.getShare() == null || Boolean.FALSE.equals(reqVo.getShare());
        if (ShareSettingTypeEnum.SHARE.equals(reqVo.getSettingType()) && closeShare) {
            settingShareClose(dcLibrary);
        }

        // 修改
        update(Wrappers.<DcLibrary>lambdaUpdate()
                .set(DcLibrary::getShare, dcLibrary.getShare())
                .set(DcLibrary::getShareLink, dcLibrary.getShareLink())
                .set(DcLibrary::getShareValidityType, dcLibrary.getShareValidityType())
                .set(DcLibrary::getSharePassword, dcLibrary.getSharePassword())
                .set(DcLibrary::getShareKey, dcLibrary.getShareKey())
                .set(DcLibrary::getShareEndTime, dcLibrary.getShareEndTime())
                .eq(DcLibrary::getId, dcLibrary.getId()));

        // 修改redis缓存
        updateShareCache(oldKey, dcLibrary);
        return dcLibrary;
    }

    /**
     * 生成分享链接
     *
     * @param reqVo
     * @param dcLibrary
     */
    private void settingShareCreateUrl(ShareSaveReqVo reqVo, DcLibrary dcLibrary) {
        // 生成分享key，每次分享都生成不同的key。一个资源同时只存在一个分享key
        String key = DigestUtil.md5Hex(new StringBuilder()
                .append(reqVo.getId())
                .append(System.currentTimeMillis())
                .append(RandomUtil.randomInt(1000, 9999))
                .toString());

        // 分享链接参数：security=是否加密&key=分享key
        String link = buildShareLink(reqVo.getPwd(), key);

        //保存修改入库（未设置有效期，默认“永久有效”,已设置有效期，则不改变有效期设置）
        dcLibrary.setShare(Boolean.TRUE)
                .setShareLink(link)
                .setShareKey(key)
                .setShareValidityType(dcLibrary.getShareValidityType() == null ? DcLibraryShareValidityTypeEnum.PERPETUAL : dcLibrary.getShareValidityType());
    }

    /**
     * 构造分享链接
     *
     * @param pwd 分享密码
     * @param key 分享key
     * @return
     */
    private String buildShareLink(String pwd, String key) {
        return new StringBuilder()
                .append("security=").append(StringUtils.isNotBlank(pwd))
                .append("&key=").append(key)
                .toString();
    }

    /**
     * 设置分享密码
     *
     * @param reqVo
     * @param dcLibrary
     */
    private void settingSharePwd(ShareSaveReqVo reqVo, DcLibrary dcLibrary) {
        //直接使用数字密码
        dcLibrary.setSharePassword(reqVo.getPwd());
        // 修改分享链接的security
        dcLibrary.setShareLink(buildShareLink(dcLibrary.getSharePassword(), dcLibrary.getShareKey()));
    }

    /**
     * 设置分享时效
     *
     * @param reqVo
     * @param dcLibrary
     */
    private void settingShareValidity(ShareSaveReqVo reqVo, DcLibrary dcLibrary) {
        LocalDateTime shareEndTime = null;
        if (null == reqVo.getValidityType()) {
            throw new BusinessException("时效类型不能为空");
        }
        switch (reqVo.getValidityType()) {
            case PERPETUAL:
                // 永久有效，不设置过期时间
                break;
            case ONE_DAY:
                shareEndTime = LocalDateTime.now().plusDays(1);
                break;
            case SEVEN_DAY:
                shareEndTime = LocalDateTime.now().plusDays(7);
                break;
            case THIRTY_DAY:
                shareEndTime = LocalDateTime.now().plusDays(30);
                break;
            default:
                // 默认永久有效，不设置过期时间
                break;
        }
        dcLibrary.setShareEndTime(shareEndTime);
    }

    /**
     * 停止分享
     *
     * @param dcLibrary
     */
    private void settingShareClose(DcLibrary dcLibrary) {
        dcLibrary.setShare(Boolean.FALSE)
                .setShareLink(null)
                .setShareValidityType(null)
                .setSharePassword(null)
                .setShareKey(null)
                .setShareEndTime(null);
    }


    /**
     * 保存到redis
     *
     * @param oldKey    原有分享key
     * @param dcLibrary 分享信息
     */
    private void updateShareCache(String oldKey, DcLibrary dcLibrary) {
        DcLibrary share = new DcLibrary();
        share.setId(dcLibrary.getId())
                .setShare(dcLibrary.getShare())
                .setShareLink(dcLibrary.getShareLink())
                .setShareValidityType(dcLibrary.getShareValidityType())
                .setSharePassword(dcLibrary.getSharePassword())
                .setShareEndTime(dcLibrary.getShareEndTime());
        String content = JSON.toJSONString(share);
        // redis缓存key格式："knowledge:link:key:{key}"
        String knowledgeLinkKey = DcLibraryUtil.getKnowledgeLinkKey(oldKey);
        // 关闭分享，删除缓存
        if (Boolean.FALSE.equals(share.getShare())) {
            redisUtils.del(knowledgeLinkKey);
            return;
        }

        // 若分享key变更，则删除旧分享key缓存
        if (!dcLibrary.getShareKey().equals(oldKey)) {
            redisUtils.del(knowledgeLinkKey);
        }

        // 分享开启的状态，变更缓存
        knowledgeLinkKey = DcLibraryUtil.getKnowledgeLinkKey(dcLibrary.getShareKey());
        if (dcLibrary.getShareEndTime() == null) {
            // 无有效时间，则不设置ttl
            redisUtils.set(knowledgeLinkKey, content);
        } else {
            // 设置有效时间
            long until = LocalDateTime.now().until(dcLibrary.getShareEndTime(), ChronoUnit.SECONDS);
            redisUtils.setExpire(knowledgeLinkKey, content, until, TimeUnit.SECONDS);
        }
    }


    @Override
    public ShareCheckResVo checkShare(ShareCheckReqVo reqVo) {
        ShareCheckResVo resVo = new ShareCheckResVo();
        resVo.setCheck(Boolean.FALSE);

        // 1. 校验分享是否存在
        String knowledgeLinkKey = DcLibraryUtil.getKnowledgeLinkKey(reqVo.getKey());
        Object document = redisUtils.get(knowledgeLinkKey);
        if (document == null) {
            // 已停止分享,或分享不存在
            resVo.setShareStatus(Boolean.FALSE);
            return resVo;
        }
        DcLibrary linkLibrary = JSON.parseObject(document.toString(), DcLibrary.class);
        String id = linkLibrary.getId();
        DcLibrary library = getById(id);
        if (library == null || !library.getShare()) {
            // 已停止分享,或分享不存在
            resVo.setShareStatus(Boolean.FALSE);
            return resVo;
        }
        resVo.setId(id);
        resVo.setKnowledgeId(DcLibraryTypeEnum.knowledge.equals(library.getType()) ? id : library.getKnowledgeId());
        resVo.setTenantId(library.getTenantId());
        resVo.setType(library.getType());
        resVo.setShareStatus(Boolean.TRUE);
        resVo.setCheck(Boolean.TRUE);

        // 2. 校验密码
        resVo.setNeedPwd(Boolean.FALSE);
        // 存在分享密码，则校验密码
        if (StringUtils.isNotBlank(library.getSharePassword())) {
            resVo.setNeedPwd(Boolean.TRUE);

            if (!library.getSharePassword().equals(reqVo.getPwd())) {
                resVo.setCheck(Boolean.FALSE);
            }
        }

        return resVo;
    }


    /**
     * 查询所有的子集
     *
     * @param dcLibrary 知识库
     * @param userId    用户
     * @return 知识库
     */
    @Override
    public DcLibrary getSubList(String userId, DcLibrary dcLibrary) {
        //不显示这些数据
        if (dcLibrary.getType().equals(DcLibraryTypeEnum.knowledge)) {
            //是不是所有者
            dcLibrary.setIsOwner(dcLibraryUserMapper.selectCount(Wrappers.<DcLibraryUser>lambdaQuery().eq(DcLibraryUser::getDcLibraryId, dcLibrary.getId()).eq(DcLibraryUser::getUserId, userId).eq(DcLibraryUser::getRole, DcLibraryUserRoleEnum.owner)) > 0);
            if (Boolean.FALSE.equals(dcLibrary.getIsOwner())) {
                dcLibrary.setSharePassword(null);
            }
        }
        //获取当前节点是否有子集 目录
        List<DcLibrary> children = list(Wrappers.<DcLibrary>lambdaQuery().eq(DcLibrary::getType, DcLibraryTypeEnum.directory).eq(DcLibrary::getParentId, dcLibrary.getId())
                .select(DcLibrary::getId, DcLibrary::getParentId, DcLibrary::getName, DcLibrary::getType, DcLibrary::getOrderId, DcLibrary::getDescription, DcLibrary::getShare));
        //排序
        children = children.stream().sorted(Comparator.comparingInt(DcLibrary::getOrderId))

                .collect(Collectors.toList());

        // 查询节点下的文件集合
        List<DcLibrary> documents = list(Wrappers.<DcLibrary>lambdaQuery()
                .notIn(DcLibrary::getType, DcLibraryTypeEnum.knowledge, DcLibraryTypeEnum.directory)
                .eq(DcLibrary::getParentId, dcLibrary.getId())
                .orderByDesc(DcLibrary::getCreateTime).orderByAsc(DcLibrary::getOrderId)
        ).stream()
                .peek(e -> {
                    //如果是其它类型的，直接拼接网络地址
                    if (e.getType().equals(DcLibraryTypeEnum.document_unrecognized) && ObjectNull.isNotNull(e.getFilePath(), e.getBucketName())) {
                        String fileLink = ossTemplate.fileLink(e.getFilePath(), e.getBucketName());
                        e.setContent(fileLink);
                    }
                })
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(documents)) {
//            // 编辑次数越多，path版本数据量越大。只返回最新版本path
//            documents.stream().map(c -> {
//                JSONArray filePaths = JSONUtil.parseArray(StrUtil.blankToDefault(c.getFilePath(), "[]"));
//                String currentPath = CollectionUtils.isEmpty(filePaths) ? null : JSONUtil.toJsonStr(filePaths.get(filePaths.size() - 1));
//                c.setFilePath(currentPath);
//                return c;
//            }).collect(Collectors.toList());
            children.addAll(documents);
        }
        //将子集添加到当前节点下
        dcLibrary.setChildren(children);
        if (ObjectUtil.isNotEmpty(children)) {
            //递归子集继续添加
            children.stream()
                    .filter(c -> c.getType().equals(DcLibraryTypeEnum.knowledge) || c.getType().equals(DcLibraryTypeEnum.directory))
                    .forEach(dc -> this.getSubList(userId, dc));
        }
        //返回知识库
        return dcLibrary;
    }


    @Override
    public List<DcLibrary> tree(String userId, String id, ShareCheckReqVo shareReqVo) {
        // 有权限查询的知识库id集合
        List<String> ids = new ArrayList<>();

        // 查询分享数据
        if (StringUtils.isNotBlank(shareReqVo.getKey())) {
            ids = Arrays.asList(shareComponent.getId(shareReqVo));
        } else {
            // 查询当前用户相关的所有的知识库
            if (StringUtils.isNotBlank(userId)) {
                ids = Optional.ofNullable(dcLibraryUserMapper.selectList(Wrappers.query(new DcLibraryUser().setUserId(userId)))).orElse(Collections.emptyList())
                        .stream()
                        .map(DcLibraryUser::getDcLibraryId)
                        .distinct()
                        .collect(Collectors.toList());
            }
            // 查询指定知识库目录树
            if (StringUtils.isNotBlank(id)) {
                // 不是知识库成员，且知识库不是完全开放的,且知识库不是开放给当前租户所有用户的，则直接返回空
                String tenantId = TenantContextHolder.getTenantId();
                DcLibrary dcLibrary = getOne(Wrappers.<DcLibrary>lambdaQuery().eq(DcLibrary::getId, id));
                if (dcLibrary == null) {
                    return Collections.emptyList();
                }
                boolean readRole = DcLibraryReadEnum.all.equals(dcLibrary.getShareRole());
                boolean registerRole = DcLibraryReadEnum.register.equals(dcLibrary.getShareRole()) && dcLibrary.getTenantId().equals(tenantId);
                if (!ids.contains(id) && !readRole && !registerRole) {
                    return Collections.emptyList();
                }
                ids.clear();
                ids.add(id);
            }
        }

        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        List<DcLibrary> knowledges = list(Wrappers.<DcLibrary>lambdaQuery().in(DcLibrary::getId, ids));

        return knowledges.parallelStream()
                //获取树形
                .map(dc -> this.getSubList(userId, dc))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String userId, String id) {
        DcLibrary dcLibrary = this.getById(id);
        if (dcLibrary != null) {
            //递归删除 ,解散该知识库相关的成员
            this.removeDc(id);
        }
    }

    @Override
    public DcLibrary getKnowledgeByChildren(String id) {
        DcLibrary library = this.getById(id);
        if (library == null) {
            return null;
        } else if (library.getType() == DcLibraryTypeEnum.knowledge) {
            return library;
        } else {
            return getById(library.getKnowledgeId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DcLibrary put(UserDto userDto, DcLibrary dto) {
        //判断是不是重命名
        DcLibrary library = this.getById(dto.getId());
        if (library == null) {
            throw new BusinessException("数据不存在");
        }
        String name = dto.getName();
        Function<String, DcLibrary> renameFunction = rename -> {
            boolean flag = true;
            //不为空 该层级下没有该名称
            Assert.isTrue(rename.length() <= 125, "标题太长啦！");
            if (StrUtil.isNotBlank(rename) && !rename.equals(library.getName())) {
                if (StrUtil.isNotEmpty(dto.getDescription()) && !dto.getDescription().equals(library.getDescription())) {
                    flag = false;
                    library.setDescription(dto.getDescription());
                }
                this.updateById(library.setName(rename));
            }
            if (flag && StrUtil.isNotEmpty(dto.getDescription()) && !dto.getDescription().equals(library.getDescription())) {
                this.update(Wrappers.<DcLibrary>lambdaUpdate().eq(DcLibrary::getId, library.getId()).set(DcLibrary::getDescription, dto.getDescription()));
            }
            return library;
        };

        //不是知识库只会是重命名
        if (!library.getType().equals(DcLibraryTypeEnum.knowledge)) {
            //权限校验-只有管理员或所有者可以重命名
            roleComponent.checkOperationAuthority(OperationEnum.DC_LIBRARY_RENAME, userDto.getId(), dto.getId());
            renameFunction.apply(name);
        } else {
            //是知识库，修改知识库信息
            //权限校验-只有所有者可以设置知识库
            roleComponent.checkOperationAuthority(OperationEnum.DC_LIBRARY_SETTING, userDto.getId(), dto.getId());
            library.setName(dto.getName());
            library.setColor(dto.getColor());
            library.setDescription(dto.getDescription());
            library.setShareRole(Optional.ofNullable(dto.getShareRole()).orElseThrow(() -> new BusinessException("阅读权限不能为空")));
            library.setReadNotify(dto.getReadNotify());

            this.updateById(library);
        }
        return library;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void putUser(String documentId, List<DcLibraryUser> dcLibraryUsers) {
        Set<String> documentUserIds = dcLibraryUserMapper.selectList(Wrappers.<DcLibraryUser>lambdaQuery().eq(DcLibraryUser::getDcLibraryId, documentId).select(DcLibraryUser::getUserId)).stream().map(DcLibraryUser::getUserId).collect(Collectors.toSet());
        Optional.ofNullable(dcLibraryUsers).orElse(Collections.emptyList()).stream().distinct()
                .filter(f -> ObjectUtil.isNotEmpty(f.getUserId()) && ObjectUtil.isNotEmpty(f.getDcLibraryId()))
                .filter(f -> !documentUserIds.contains(f.getUserId()))
                //全部设置为普通用户 后面赋权时变更
                .map(f -> f.setRole(DcLibraryUserRoleEnum.member))
                .forEach(dcLibraryUserMapper::insert);
    }

    @Override
    public Page<DcLibraryUser> queryUser(Page<DcLibraryUser> page, String id, String userId) {
        DcLibrary dcLibrary = this.getById(id);
        page = dcLibraryUserMapper.selectPage(page, Wrappers.query(new DcLibraryUser().setDcLibraryId(id)));
        page.getRecords().parallelStream().forEach(e -> e.setRemark(e.getUserId().equals(userId) ? "自己" : (e.getUserId().equals(dcLibrary.getEditingBy()) ? "正在编辑中" : "")));
        return page;
    }

    @Override
    public void deleteMemberById(String documentId, String userId) {
        DcLibraryUser libraryUser = dcLibraryUserMapper.selectOne(Wrappers.<DcLibraryUser>lambdaQuery().eq(DcLibraryUser::getDcLibraryId, documentId).eq(DcLibraryUser::getUserId, userId));
        if (libraryUser != null) {
            if (libraryUser.getRole() == DcLibraryUserRoleEnum.owner) {
                throw new BusinessException("不能删除所有者");
            }
            dcLibraryUserMapper.delete(Wrappers.update(new DcLibraryUser().setDcLibraryId(documentId).setUserId(userId)));
        }
    }

    @Override
    public List<String> subdirectory(String id) {
        DcLibrary library = getById(id);
        ArrayList<String> results = new ArrayList<>();
        if (null != library) {
            subdirectory(results, Collections.singletonList(library.getId()));
            return results;
        }
        return results;
    }

    public void subdirectory(List<String> results, List<String> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        if (ObjectUtil.isNull(results)) {
            results = new ArrayList<>();
        }
        //寻找儿子
        List<String> subset = this.list(Wrappers.<DcLibrary>lambdaQuery().in(DcLibrary::getParentId, ids).select(DcLibrary::getId)).stream().map(DcLibrary::getId).collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(subset)) {
            //添加儿子
            results.addAll(subset);
            subdirectory(results, subset);
        }
    }


    @Override
    public Page<DcLibrary> queryKnowledge(Page<DcLibrary> page, String userId) {
        // 获取登录用户有权限知识库id
        LambdaQueryWrapper<DcLibraryUser> query = Wrappers.<DcLibraryUser>lambdaQuery()
                .eq(DcLibraryUser::getUserId, userId)
                .select(DcLibraryUser::getDcLibraryId, DcLibraryUser::getRole);
        List<DcLibraryUser> dcLibraryUsers = dcLibraryUserMapper.selectList(query);
        Set<String> knowledgeIds = CollectionUtils.isEmpty(dcLibraryUsers) ? null : dcLibraryUsers.stream().map(DcLibraryUser::getDcLibraryId).collect(Collectors.toSet());

        String currentTenantId = TenantContextHolder.getTenantId();
        TenantContextHolder.clear();
        // 获取可查看的知识库信息(作为成员的知识库 + 全租户下完全开放的知识库 + 当前租户下开放的知识库)
        LambdaQueryWrapper<DcLibrary> queryKnowledge = Wrappers.<DcLibrary>lambdaQuery()
                .in(CollectionUtils.isNotEmpty(knowledgeIds), DcLibrary::getId, knowledgeIds)
                .or().eq(DcLibrary::getShareRole, DcLibraryReadEnum.all)
                .or(itemWrapper -> {
                    itemWrapper.eq(DcLibrary::getShareRole, DcLibraryReadEnum.register);
                    itemWrapper.eq(DcLibrary::getTenantId, currentTenantId);
                })
                .orderByAsc(DcLibrary::getOrderId).orderByDesc(DcLibrary::getCreateTime)
          ;
        this.page(page, queryKnowledge);

        if (CollectionUtils.isEmpty(page.getRecords())) {
            return page;
        }

        // 封装表结构之外的数据
        // 封装是否是所有者
        page.getRecords().forEach(dcLibrary ->
                dcLibrary.setIsOwner(dcLibraryUsers.stream().anyMatch(dcUser -> dcUser.getDcLibraryId().equals(dcLibrary.getId()) && DcLibraryUserRoleEnum.owner.equals(dcUser.getRole())))
        );
        return page;
    }

    @Override
    public Page<DcLibrary> queryOwnerKnowledge(Page page, String userId) {
        // 查询自己拥有的知识库id
        LambdaQueryWrapper<DcLibraryUser> query = Wrappers.<DcLibraryUser>lambdaQuery()
                .eq(DcLibraryUser::getUserId, userId)
                .eq(DcLibraryUser::getRole, DcLibraryUserRoleEnum.owner)
                .select(DcLibraryUser::getDcLibraryId);
        List<DcLibraryUser> dcLibraryUsers = dcLibraryUserMapper.selectList(query);
        Set<String> knowledgeIds = CollectionUtils.isEmpty(dcLibraryUsers) ? null : dcLibraryUsers.stream().map(DcLibraryUser::getDcLibraryId).collect(Collectors.toSet());

        // 查询知识库信息
        if (CollectionUtils.isNotEmpty(knowledgeIds)) {
            LambdaQueryWrapper<DcLibrary> queryKnowledge = Wrappers.<DcLibrary>lambdaQuery()
                    .in(DcLibrary::getId, knowledgeIds)
                    .orderByAsc(DcLibrary::getOrderId)
                    .orderByDesc(DcLibrary::getCreateTime);
            this.page(page, queryKnowledge);
        }
        return page;
    }

    @Override
    public Set<String> getAllChildDcLibraryId(String id) {
        return getAllChildDcLibraryId(new ArrayList<>(Arrays.asList(id)));
    }

    @Override
    public Set<String> getAllChildDcLibraryId(List<String> ids) {
        Set<String> childIds = new HashSet<>();
        getAllChildIds(childIds, ids);
        return childIds;
    }

    /**
     * 递归获取指定知识库下所有子节点(不包括文档)
     *
     * @param childIds 子节点集合
     * @param ids      上级节点
     */
    private void getAllChildIds(Set<String> childIds, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        childIds.addAll(ids);

        // 查询下级节点
        LambdaQueryWrapper<DcLibrary> query = Wrappers.<DcLibrary>lambdaQuery()
                .in(DcLibrary::getParentId, ids)
                .and(wrapper ->
                        wrapper.eq(DcLibrary::getType, DcLibraryTypeEnum.knowledge)
                                .or()
                                .eq(DcLibrary::getType, DcLibraryTypeEnum.directory)
                )
                .select(DcLibrary::getId);
        List<DcLibrary> dcLibraries = this.list(query);
        if (CollectionUtils.isEmpty(dcLibraries)) {
            return;
        }
        // 递归查询下级节点
        List<String> childDcIds = dcLibraries.stream().map(DcLibrary::getId).collect(Collectors.toList());
        getAllChildIds(childIds, childDcIds);
    }

    @Override
    public List<DcLibrary> getDocumentByIds(Set<String> ids) {
        LambdaQueryWrapper<DcLibrary> queryWrapper = Wrappers.<DcLibrary>lambdaQuery()
                .in(DcLibrary::getParentId, ids)
                .notIn(DcLibrary::getType, DcLibraryTypeEnum.knowledge, DcLibraryTypeEnum.directory)
                .orderByDesc(DcLibrary::getCreateTime).orderByAsc(DcLibrary::getOrderId);
        return list(queryWrapper);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveContent(String userId, DcLibrary dcLibrary, String documentId) {

        if (dcLibrary == null || dcLibrary.getType() == DcLibraryTypeEnum.directory || dcLibrary.getType() == DcLibraryTypeEnum.knowledge) {
            throw new BusinessException("文档不存在，参数错误");
        }

        // 权限校验
        roleComponent.checkOperationAuthority(OperationEnum.DC_LIBRARY_DOCUMENT_SAVE, userId, documentId);

        updateById(dcLibrary);
        // 发送查看提醒
        notifyService.autoSendReadNotify(userId, documentId);


    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void uploadDocument(String userId, String parentId, String originalFilename, DcLibrary dcLibrary) {
        if (dcLibrary == null || !Arrays.asList(DcLibraryTypeEnum.knowledge, DcLibraryTypeEnum.directory).contains(dcLibrary.getType())) {
            throw new BusinessException("目录不存在");
        }
        // 权限校验（上传文档 = 新增文档。权限同add方法）
        roleComponent.checkOperationAuthority(OperationEnum.DC_LIBRARY_ADD, userId, dcLibrary.getId());
        // 新增知识库文件
        DcLibrary saveLibrary = new DcLibrary();
        saveLibrary.setType(DcLibraryTypeEnum.document_unrecognized);
        saveLibrary.setName(originalFilename);
        saveLibrary.setParentId(parentId);
        saveLibrary.setFilePath(dcLibrary.getFilePath());
        saveLibrary.setBucketName(dcLibrary.getBucketName());
        saveLibrary.setKnowledgeId(DcLibraryTypeEnum.knowledge.equals(dcLibrary.getType()) ? dcLibrary.getId() : dcLibrary.getKnowledgeId());
        save(saveLibrary);
    }

}
