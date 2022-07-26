package cn.bctools.document.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import cn.bctools.document.entity.DcLibrary;
import lombok.SneakyThrows;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

/**
 * @author Administrator
 */
public class FileOssUtils {

    public static String getMultipartFileName(DcLibrary dcLibrary, String content) {
        byte[] serialize = ObjectUtil.serialize(content);
        // 上传文件，并保存文件地址
        // 目录暂为 "{创建人Id}/{文档id}"
        return IdUtil.fastSimpleUUID().concat(StringPool.DOT).concat(dcLibrary.getType().value);
//        return new MockMultipartFile(pathname, pathname, "text/plain; charset=ISO-8859-1", serialize);
    }


    @SneakyThrows
    public static InputStream getMultipartInputStream(String pathname, String content) {
        byte[] serialize = ObjectUtil.serialize(content);
        return new MockMultipartFile(pathname, pathname, "text/plain; charset=ISO-8859-1", serialize).getInputStream();
    }
}
