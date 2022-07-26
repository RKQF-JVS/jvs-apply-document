package cn.bctools.document.repository;

import cn.bctools.document.po.DocumentEsPo;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @Author: ZhuXiaoKang
 * @Description:
 */
@Repository
public interface DocumentRepository extends PagingAndSortingRepository<DocumentEsPo, String> {
}
