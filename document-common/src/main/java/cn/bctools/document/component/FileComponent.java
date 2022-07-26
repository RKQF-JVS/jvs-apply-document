//package cn.bctools.document.component;
//
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.json.JSONArray;
//import cn.hutool.json.JSONObject;
//import cn.hutool.json.JSONUtil;
//import cn.bctools.common.exception.BusinessException;
//import cn.bctools.common.utils.function.Get;
//import cn.bctools.document.entity.DcLibrary;
//import cn.bctools.document.entity.enums.DcLibraryTypeEnum;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.ByteArrayOutputStream;
//import java.util.Map;
//
///**
// * @Author: ZhuXiaoKang
// * @Description: File
// */
//
//@Slf4j
//@Component
//public class FileComponent {
//
//    @Autowired
//    private FileApi fileApi;
//
//    /**
//     * 文件路径解析
//     * @param dcFilePath 知识库文件路径
//     * @return
//     */
//    public String[] getFilePathArray(String dcFilePath) {
//        JSONArray jsonArray = JSONUtil.parseArray(StrUtil.blankToDefault(dcFilePath, "[]"));
//        if (!jsonArray.isEmpty()) {
//            String[] filePath = ((Map<String, String>) jsonArray.get(jsonArray.size() - 1)).get(Get.name(DcLibrary::getFilePath)).split(",", -1);
//            return filePath;
//        }
//        return null;
//    }
//
//    /**
//     * 获取知识库文件内容
//     * @param dcFilePath 知识库文件路径
//     * @return 文件内容
//     */
//    public byte[] getDcLibraryFileBytes(String dcFilePath) {
//        try {
//            String[] filePathArray = getFilePathArray(dcFilePath);
//            if (filePathArray != null) {
//                return getFileBytes(filePathArray[0], filePathArray[1]);
//            }
//            return null;
//        } catch (Exception e) {
//            log.error("获取知识库文件内容失败. exception: {}", e);
//            throw new BusinessException("获取文件内容失败");
//        }
//    }
//
//    /**
//     * 获取文件内容
//     *
//     * @param bucketName
//     * @param fileName
//     * @return
//     */
//    public byte[] getFileBytes(String bucketName, String fileName) {
//        try {
//            return fileApi.bytes(bucketName, fileName).getData();
//        } catch (Exception e) {
//            log.error("调用FileApi失败. exception:{}", e);
//            throw new BusinessException("获取文件内容失败");
//        }
//    }
//
//    /**
//     * 上传文件
//     * @param file
//     * @param pathname
//     * @param moduleName
//     * @return
//     */
//    public FileNameDto uploadFile(MultipartFile file, String pathname, String moduleName) {
//        try {
//            R<FileNameDto> r = fileApi.uploadFile(file, pathname, "knowledge-library", moduleName);
//            return r.getData();
//        } catch (Exception e) {
//            log.error("调用FileApi失败. exception:{}", e);
//        }
//        return null;
//    }
//
//
//    /**
//     * 下载单个文档
//     * 下载由前端实现，后端暂不实现
//     * @param fileName 文件名
//     * @param typeEnum 文档类型
//     * @param content 文件内容
//     * @return
//     */
//    @Deprecated
//    public byte[] downLoadFile(String fileName, DcLibraryTypeEnum typeEnum, String content) {
//        byte[] downloadByte = null;
//        switch (typeEnum) {
//            case document_html:
//                // 富文本文档
//                return convertHtmlToWord(content);
//            case document_map:
//                // 脑图文档
//                return convertJsonToExcel(content);
//            case document_flow:
//                // 流程文档
//                return downloadByte;
//            case document_xlsx:
//                // 表格文档
//                return downloadByte;
//            default:
//                // 未识别的文档格式，直接下载
//                return downloadByte;
//        }
//    }
//
//
//    /**
//     * 构建下载文件名
//     * 下载由前端实现，后端暂不实现
//     * @param name 知识库文档名称
//     * @param typeEnum 文档类型
//     * @return 文件名
//     */
//    @Deprecated
//    public String buildDownLoadFileName(String name, DcLibraryTypeEnum typeEnum) {
//        switch (typeEnum) {
//            case document_html:
//                // 富文本文档
//                return name + ".doc";
//            case document_map:
//                // 脑图文档
//                return "";
//            case document_flow:
//                // 流程文档
//                return "";
//            case document_xlsx:
//                // 表格文档
//                return "";
//            default:
//                // 未识别的文档格式，直接下载
//                return "";
//        }
//    }
//
//    /**
//     * 富文本json转word
//     * 下载由前端实现，后端暂不实现
//     * @param html 内容
//     * @return
//     */
//    @Deprecated
//    @SneakyThrows
//    private static byte[] convertHtmlToWord(String html) {
//        html = html.replace("&lt;", "<")
//                .replace("&gt;", ">")
//                .replace("&quot;", "\"")
//                .replace("&amp;", "&");
//        String content="<html><body>"+html+"</body></html>";
//
//        return content.getBytes("GBK");
//    }
//
//    /**
//     * excel json转excel
//     * 下载由前端实现，后端暂不实现
//     * @param json
//     * @return
//     */
//    @Deprecated
//    @SneakyThrows
//    private static byte[] convertJsonToExcel(String json) {
//        //创建工作薄对象
//        HSSFWorkbook workbook = new HSSFWorkbook();
//
//        // 解析JSON，构建excel
//        // TODO 样式设置
//        JSONArray contentArray = JSONUtil.parseArray(json);
//        for (Object o : contentArray) {
//            JSONObject sheetObj = (JSONObject) o;
//            Integer sheetOrder = Integer.parseInt(sheetObj.getStr("order"));
//            String sheetName = sheetObj.getStr("name");
//            JSONArray jsonArray = sheetObj.getJSONArray("celldata");
//
//            // 创建工作表对象
//            HSSFSheet sheet = workbook.createSheet();
//
//            // 从第一行开始
//            int currentRow = 0;
//            HSSFRow row = sheet.createRow(currentRow);
//            for (Object cellObj : jsonArray) {
//                JSONObject cellDataObj = (JSONObject) cellObj;
//                // 创建行
//                int rowNumber = cellDataObj.getInt("r");
//                if (rowNumber != currentRow) {
//                    row = sheet.createRow(rowNumber);
//                }
//                // 设置值
//                row.createCell(cellDataObj.getInt("c")).setCellValue(cellDataObj.getJSONObject("v").getStr("v"));
//            }
//
//            //设置sheet的Name
//            workbook.setSheetName(sheetOrder,sheetName);
//        }
//
//        // 转为字节流
//        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//        workbook.write(byteStream);
//        return byteStream.toByteArray();
//    }
//
//}
