package cn.bctools.document.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.entity.DcLibraryUser;
import cn.bctools.document.vo.req.DcLibraryAddReqVo;
import cn.bctools.document.vo.req.ShareCheckReqVo;
import cn.bctools.document.vo.req.ShareSaveReqVo;
import cn.bctools.document.vo.res.ShareCheckResVo;

import java.util.List;
import java.util.Set;

/**
 * @author auto
 */
public interface DcLibraryService extends IService<DcLibrary> {

    /**
     * 删除一个知识库，或目录，或文档
     *
     * @param id 知识库ID，或目录ID，或文档ID
     */
    void removeDc(String id);

    /**
     * 新增知识库 目录 或文档
     *
     * @param userDto   当前用户
     * @param reqVo    新增入参
     * @return
     */
    DcLibrary add(UserDto userDto, DcLibraryAddReqVo reqVo);


    /**
     * 分享设置
     * @param reqVo 分享设置
     * @return
     */
    DcLibrary settingShare(ShareSaveReqVo reqVo);

    /**
     * 校验分享
     * @param reqVo
     * @return
     */
    ShareCheckResVo checkShare(ShareCheckReqVo reqVo);

    /**
     * 查询所有的子集
     *
     * @param dcLibrary 知识库
     * @param userId    用户ID
     * @return 知识库
     */
    DcLibrary getSubList(String userId, DcLibrary dcLibrary);

    /**
     * 获取知识库树形目录结构
     *
     * @param userId     用户id
     * @param id         知识库id
     * @param shareReqVo 分享请求（用以判是否查询分享数据）
     * @return 树形目录结构
     */
    List<DcLibrary> tree(String userId, String id, ShareCheckReqVo shareReqVo);

    /**
     * 删除知识库/目录/文档
     *
     * @param userId 用户id
     * @param id     操作数据id
     */
    void delete(String userId, String id);

    /**
     * 获取子集属于哪个知识库
     *
     * @param id 子集id
     * @return 知识库id
     */
    DcLibrary getKnowledgeByChildren(String id);

    /**
     * 重命名知识库/目录/文档，或设置知识库
     *
     * @param userDto 登录用户
     * @param dto     知识库/目录/文档
     * @return 知识库/目录/文档
     */
    DcLibrary put(UserDto userDto, DcLibrary dto);

    /**
     * 添加知识库成员
     *
     * @param documentId     知识库id
     * @param dcLibraryUsers 添加成员
     */
    void putUser(String documentId, List<DcLibraryUser> dcLibraryUsers);

    /**
     * 查询知识库成员
     *
     * @param id     知识库id
     * @param page   分页
     * @param userId 操作人id
     * @return 知识库成员
     */
    Page<DcLibraryUser> queryUser(Page<DcLibraryUser> page, String id, String userId);

    /**
     * 删除知识库成员
     *
     * @param documentId 知识库
     * @param userId     用户
     */
    void deleteMemberById(String documentId, String userId);

    /**
     * 递归子目录
     *
     * @param id 当前目录id
     * @return results 返回子目录
     */
    List<String> subdirectory(String id);

    /**
     * 查询用户有权限的知识库
     *
     * @param page
     * @param userId 用户id
     * @return
     */
    Page<DcLibrary> queryKnowledge(Page<DcLibrary> page, String userId);

    /**
     * 查询用户自己的知识库
     *
     * @param page
     * @param userId 用户id
     * @return
     */
    Page<DcLibrary> queryOwnerKnowledge(Page page, String userId);

    /**
     * 获取指定知识库所有下级节点id(不包括文档)
     *
     * @param id 目录id
     * @return
     */
    Set<String> getAllChildDcLibraryId(String id);


    /**
     * 获取多个指定知识库所有下级节点id(不包括文档)
     *
     * @param ids 目录id集合
     * @return
     */
    Set<String> getAllChildDcLibraryId(List<String> ids);

    /**
     * 获取指定目录下所有文档集合
     *
     * @param ids 目录集合
     * @return 文档i合
     */
    List<DcLibrary> getDocumentByIds(Set<String> ids);

    /**
     * 保存文档
     * @param userId 登录用户
     * @param dcLibrary 文档内容
     * @param documentId 文档id
     */
    void saveContent(String userId, DcLibrary dcLibrary, String documentId);

    /**
     * 上传文件
     * @param userId  登录用户
     * @param originalFilename  文件名
     * @param parentId 上级目录id
     * @param dcLibrary
     */
    void uploadDocument(String userId, String parentId, String originalFilename, DcLibrary dcLibrary);

}
