package cn.bctools.document.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.bctools.common.entity.dto.UserDto;
import cn.bctools.common.utils.R;
import cn.bctools.common.utils.TenantContextHolder;
import cn.bctools.document.component.UserComponent;
import cn.bctools.document.entity.DcLibrary;
import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
import cn.bctools.document.mapper.DcLibraryMapper;
import cn.bctools.document.mapper.DcLibraryUserMapper;
import cn.bctools.document.po.DocumentEsPo;
import cn.bctools.document.service.DocumentElasticService;
import cn.bctools.log.annotation.Log;
import cn.bctools.oss.template.OssTemplate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: ZhuXiaoKang
 * @Description: 数据同步
 */

@Slf4j
@Api(tags = "数据同步")
@RestController
@RequestMapping(value = "/dcLibrary/sync")
@AllArgsConstructor
public class DataSyncController {

    DocumentElasticService documentElasticService;
    ElasticsearchRestTemplate esTemplate;
    DcLibraryUserMapper libraryUserMapper;
    DcLibraryMapper libraryMapper;
    OssTemplate ossTemplate;
    UserComponent userComponent;

    @Log
    @ApiOperation(value = "全量同步知识库数据到ES[document_base_info索引]", notes = "索引名[document_base_info]")
    @PostMapping("/full/es")
    public R<String> fullSyncDocumentToElastic() {
        log.info("开始全量同步索引document_base_info");
        // 查询所有租户的知识库信息，所以要先清空tenantId
        TenantContextHolder.clear();

        // 每页同步数量
        final int size = 1000;
        // 页码
        int current = 0;
        Page<DcLibrary> batchPage = new Page<>(current, size);
        // 分批同步全量知识库文档信息
        while (current == 0 || batchPage.hasNext()) {
            // 分页查询
            current += 1;
            batchPage.setCurrent(current);
            batchPage = libraryMapper.selectPage(batchPage, Wrappers.<DcLibrary>lambdaQuery()
                    .orderByAsc(DcLibrary::getCreateTime));
            List<DcLibrary> dcLibraries = batchPage.getRecords();
            if (CollectionUtils.isEmpty(dcLibraries)) {
                break;
            }
            // 查询知识库信息
            Set<String> knowledgeIds = dcLibraries.stream().map(DcLibrary::getKnowledgeId).collect(Collectors.toSet());
            Map<String, String> knowledgeMap = libraryMapper.selectList(Wrappers.<DcLibrary>lambdaQuery()
                    .in(DcLibrary::getId, knowledgeIds)
                    .select(DcLibrary::getId, DcLibrary::getName)).stream().collect(Collectors.toMap(DcLibrary::getId, DcLibrary::getName));

            // es数据集合
            List<DocumentEsPo> documentEsPos = new ArrayList<>();

            // 遍历知识库各类文档信息，封装为es索引对象
            for (DcLibrary dcLibrary : dcLibraries) {
                // 获取文档内容。 只获取类型为document_html的内容
                String content = "";
                if (DcLibraryTypeEnum.document_html.equals(dcLibrary.getType())) {
                    String objectURL = ossTemplate.fileLink(dcLibrary.getFilePath(), dcLibrary.getBucketName());
                    byte[] bytes = HttpUtil.downloadBytes(objectURL);
                    content = bytes == null ? null : ObjectUtil.deserialize(bytes);
                }

                // 获取
                UserDto userDto = new UserDto();
                userDto.setId(dcLibrary.getCreateById());
                Map<String, UserDto> userMap = userComponent.getUserMap(Arrays.asList(dcLibrary.getCreateById()));
                userDto.setRealName(userMap.get(dcLibrary.getCreateById()).getRealName());

                DocumentEsPo documentEsPo = documentElasticService.build(userDto, dcLibrary, content, knowledgeMap.get(dcLibrary.getKnowledgeId()));
                documentEsPos.add(documentEsPo);
            }
            esTemplate.save(documentEsPos);
            log.info("全量同步索引document_base_info，总批次：{}, 当前批次：{}, 每批数量：{}", batchPage.getPages(), batchPage.getCurrent(), batchPage.getSize());
        }
        log.info("完成全量同步索引document_base_info");
        return R.ok();
    }



}
