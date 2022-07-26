package cn.bctools.document.repository;

import cn.bctools.document.po.DocumentLogEsPo;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @Author: ZhuXiaoKang
 * @Description:
 */

@Repository
public interface DocumentLogRepository extends PagingAndSortingRepository<DocumentLogEsPo, String> {
}
