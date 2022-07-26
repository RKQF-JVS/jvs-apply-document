package cn.bctools.document.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.po.DocumentEsPo;
import cn.bctools.document.po.enums.DocumentLogTypeEnum;
import cn.bctools.document.vo.req.DocumentRecentlyUpdatedVo;
import cn.bctools.document.vo.req.DocumentSearchVo;
import cn.bctools.document.vo.res.DocumentEditLogResVo;
import cn.bctools.document.vo.res.DocumentRecentlyUpdatedResVo;
import cn.bctools.document.vo.res.DocumentSearchResVo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: ZhuXiaoKang
 * @Description: 知识库-文档es服务
 */
public interface DocumentElasticService {

    /**
     * 搜索知识库文档
     *
     * @param page
     * @param documentSearchVo
     * @param userId
     * @return
     */
    Page<DocumentSearchResVo> searchDoc(Page page, DocumentSearchVo documentSearchVo, String userId);

    /**
     * 单个知识库文档保存
     *
     * @param userDto 登录用户
     * @param dcLibrary 知识库信息
     * @param content 内容
     */
    void save(UserDto userDto, DcLibrary dcLibrary, String content);

    /**
     * 封装documentEsPO 入库信息
     *
     * @param userDto       用户
     * @param dcLibrary     知识库文档信息
     * @param content       文档内容
     * @param knowledgeName 知识库名称
     * @return
     */
    DocumentEsPo build(UserDto userDto, DcLibrary dcLibrary, String content, String knowledgeName);

    /**
     * 知识库文档操作日志保存
     * @param dcLibrary 文档信息
     * @param userName  操作人
     * @param userId 操作人id
     * @param logTypeEnum 操作类型
     * @param time 操作时间
     */
    void saveLog(DcLibrary dcLibrary, String userName, String userId, DocumentLogTypeEnum logTypeEnum, LocalDateTime time);

    /**
     * 查询文档编辑记录
     *
     * @param page 分页
     * @param id 文档id
     * @return
     */
    Page<DocumentEditLogResVo> searchDocumentEditLog(Page page, String id);

    /**
     * 获取文档已读次数
     * @param id 文档id
     * @return
     */
    Long searchDocumentReadTotal(String id);

    /**
     * 查询指定知识库最近更新文档集合
     *
     * @param recentlyVo
     * @param dcLibraries
     * @return
     */
    List<DocumentRecentlyUpdatedResVo> searchDocumentRecentlyUpdate(DocumentRecentlyUpdatedVo recentlyVo, List<DcLibrary> dcLibraries);

    /**
     * 根据文档名称，搜索文档信息集合
     * @param name
     * @return
     */
    List<DocumentEsPo> searchDocumentByName(String name);

    /**
     * 删除文档【索引document_base_info】
     * @param tenantId 租户id
     * @param docId 文档id
     */
    void deleteDocument(String tenantId, String docId);

    /**
     * 部分更新知识库
     * @param dcLibrary
     */
    void updateDocumentEs(DcLibrary dcLibrary);
}
