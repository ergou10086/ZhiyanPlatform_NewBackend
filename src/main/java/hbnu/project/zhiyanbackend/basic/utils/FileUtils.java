package hbnu.project.zhiyanbackend.basic.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 文件处理工具类
 * 整合文件类型检测、文件操作、下载等功能
 *
 * @author ErgouTree
 */
public class FileUtils {

    /** 字符常量：斜杠 {@code '/'} */
    public static final char SLASH = '/';

    /** 字符常量：反斜杠 {@code '\\'} */
    public static final char BACKSLASH = '\\';

    /**
     * 文件名验证正则
     * 允许：字母、数字、中文字符（包括中文标点）、空格、连字符、下划线、点号、括号、竖线等常见字符
     * 注意：排除了路径分隔符 / \ 和其他危险字符 : * ? " < > |
     */
    public static final String FILENAME_PATTERN = "[^/\\\\:*?\"<>|]+";

    private static final Tika tika = new Tika();

    // ==================== 文件类型检测相关方法 ====================

    /**
     * 获取文件类型（后缀名）
     * <p>
     * 例如: ruoyi.txt, 返回: txt
     *
     * @param file 文件对象
     * @return 后缀（不含".")
     */
    public static String getFileType(File file) {
        if (null == file) {
            return StringUtils.EMPTY;
        }
        return getFileType(file.getName());
    }

    /**
     * 获取文件类型（后缀名）
     * <p>
     * 例如: ruoyi.txt, 返回: txt
     *
     * @param fileName 文件名
     * @return 后缀（不含".")
     */
    public static String getFileType(String fileName) {
        int separatorIndex = fileName.lastIndexOf(".");
        if (separatorIndex < 0) {
            return "";
        }
        return fileName.substring(separatorIndex + 1).toLowerCase();
    }

    /**
     * 更好的获取文件扩展名
     *
     * @param filename 文件名
     * @return 文件扩展名（小写）
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 检测文件实际类型（基于内容）
     *
     * @param content 文件内容字节数组
     * @return 文件MIME类型
     */
    public static String detectContentType(byte[] content) {
        return tika.detect(content);
    }

    /**
     * 获取文件名的后缀
     *
     * @param file 表单文件
     * @return 后缀名
     */
    public static String getExtension(MultipartFile file) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isEmpty(extension)) {
            extension = MimeTypeUtils.getExtension(Objects.requireNonNull(file.getContentType()));
        }
        return extension;
    }

    /**
     * 通过文件头信息获取文件扩展名
     *
     * @param photoByte 文件字节码
     * @return 文件扩展名
     */
    public static String getFileExtendName(byte[] photoByte) {
        if (photoByte == null || photoByte.length < 10) {
            return "JPG";
        }

        // GIF
        if ((photoByte[0] == 71) && (photoByte[1] == 73) && (photoByte[2] == 70) && (photoByte[3] == 56)
                && ((photoByte[4] == 55) || (photoByte[4] == 57)) && (photoByte[5] == 97)) {
            return "GIF";
        }
        // JPG
        else if ((photoByte[6] == 74) && (photoByte[7] == 70) && (photoByte[8] == 73) && (photoByte[9] == 70)) {
            return "JPG";
        }
        // BMP
        else if ((photoByte[0] == 66) && (photoByte[1] == 77)) {
            return "BMP";
        }
        // PNG
        else if ((photoByte[1] == 80) && (photoByte[2] == 78) && (photoByte[3] == 71)) {
            return "PNG";
        }
        return "JPG";
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // ==================== 文件操作相关方法 ====================

    /**
     * 输出指定文件的byte数组到输出流
     *
     * @param filePath 文件路径
     * @param os 输出流
     * @throws IOException 文件操作异常
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 删除是否成功
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            flag = file.delete();
        }
        return flag;
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename != null && filename.matches(FILENAME_PATTERN);
    }

    /**
     * 检查文件是否可下载
     *
     * @param resource 需要下载的文件
     * @return true 正常 false 非法
     */
    public static boolean checkAllowDownload(String resource) {
        // 禁止目录上跳级别
        if (StringUtils.contains(resource, "..")) {
            return false;
        }
        // 判断是否在允许下载的文件规则内
        return ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, getFileType(resource));
    }

    /**
     * 返回文件名（从路径中提取）
     *
     * @param filePath 文件路径
     * @return 文件名
     */
    public static String getName(String filePath) {
        if (null == filePath) {
            return null;
        }
        int len = filePath.length();
        if (0 == len) {
            return filePath;
        }
        if (isFileSeparator(filePath.charAt(len - 1))) {
            // 以分隔符结尾的去掉结尾分隔符
            len--;
        }

        int begin = 0;
        char c;
        for (int i = len - 1; i > -1; i--) {
            c = filePath.charAt(i);
            if (isFileSeparator(c)) {
                // 查找最后一个路径分隔符（/或者\）
                begin = i + 1;
                break;
            }
        }

        return filePath.substring(begin, len);
    }

    /**
     * 是否为Windows或者Linux（Unix）文件分隔符<br>
     * Windows平台下分隔符为\，Linux（Unix）为/
     *
     * @param c 字符
     * @return 是否为文件分隔符
     */
    public static boolean isFileSeparator(char c) {
        return SLASH == c || BACKSLASH == c;
    }

    // ==================== 文件下载相关方法 ====================

    /**
     * 下载文件名重新编码（兼容不同浏览器）
     *
     * @param request 请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     * @throws UnsupportedEncodingException 编码异常
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 下载文件名重新编码（现代浏览器兼容）
     *
     * @param response 响应对象
     * @param realFileName 真实文件名
     * @throws UnsupportedEncodingException 编码异常
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; filename=")
                .append(percentEncodedFileName)
                .append(";")
                .append("filename*=")
                .append("utf-8''")
                .append(percentEncodedFileName);

        response.setHeader("Content-disposition", contentDispositionValue.toString());
        response.setHeader("download-filename", percentEncodedFileName);
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     * @throws UnsupportedEncodingException 编码异常
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        return encode.replaceAll("\\+", "%20");
    }

    // ==================== 新增便捷方法 ====================

    /**
     * 获取文件大小并格式化
     *
     * @param file 文件对象
     * @return 格式化后的文件大小
     */
    public static String getFormattedFileSize(File file) {
        if (file == null || !file.exists()) {
            return "0 B";
        }
        return formatFileSize(file.length());
    }

    /**
     * 获取MultipartFile的文件大小并格式化
     *
     * @param file MultipartFile对象
     * @return 格式化后的文件大小
     */
    public static String getFormattedFileSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "0 B";
        }
        return formatFileSize(file.getSize());
    }

    /**
     * 安全获取文件名（防止路径遍历）
     *
     * @param filename 原始文件名
     * @return 安全的文件名
     */
    public static String getSafeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // 移除路径分隔符
        String safeName = filename.replaceAll("[\\\\/]", "_");
        // 确保符合文件名规范
        if (!isValidFilename(safeName)) {
            // 如果不合法，使用基础文件名
            safeName = "file_" + System.currentTimeMillis();
        }
        return safeName;
    }
}