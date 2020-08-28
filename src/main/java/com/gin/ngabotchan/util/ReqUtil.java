package com.gin.ngabotchan.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


/**
 * 请求工具类
 */

@Slf4j
public class ReqUtil {

    /**
     * 默认最大尝试次数
     */
    final static Integer MAX_TIMES = 10;
    /**
     * 默认超时时间
     */
    final static int TIME_OUT = 15 * 1000;
    /**
     * 默认请求头
     */
    private final static Map<String, String> HEADERS_DEFUALT = new HashMap<>();
    private final static Map<String, String> HEADERS_JSON = new HashMap<>();

    static {
        HEADERS_JSON.put("Content-Type", "application/json");

        HEADERS_DEFUALT.put("Accept-Language", "zh-CN,zh;q=0.9");
        HEADERS_DEFUALT.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.135 Safari/537.36");
    }


    /**
     * post请求
     */
    public static String post(String urlPrefix, String urlSuffix,
                              String portName, Map<String, String[]> paramMap,
                              String cookie, Integer timeout,
                              Map<String, String> formData, Map<String, File> fileMap,
                              Integer maxTimes, String enc) {

        Map<String, String> headers = new HashMap<>();
        headers.putAll(HEADERS_DEFUALT);
        headers.put("cookie", cookie);

        if (fileMap == null) {
            headers.putAll(HEADERS_JSON);
        }

        String url = getUrl(urlPrefix, urlSuffix, portName, paramMap, enc);

        HttpPost m = new HttpPost(url);

        setHeaderConfig(m, cookie, headers, timeout);

        setFormDataEntity(m, formData, fileMap);

        return executeRequest(m, maxTimes, enc);
    }

    /**
     * 简单post请求
     */
    public static String post(String urlPrefix, String urlSuffix,
                              String portName, Map<String, String[]> paramMap) {
        return post(urlPrefix, urlSuffix, portName, paramMap, null, null, null, null, null, null);
    }

    /**
     * get请求
     */
    public static String get(String urlPrefix, String urlSuffix,
                             String portName, Map<String, String[]> paramMap,
                             String cookie, Integer timeout,
                             Integer maxTimes, String enc) {
        Map<String, String> headers = new HashMap<>();
        headers.putAll(HEADERS_DEFUALT);

        String url = getUrl(urlPrefix, urlSuffix, portName, paramMap, enc);

        HttpGet m = new HttpGet(url);

        setHeaderConfig(m, cookie, headers, timeout);

        return executeRequest(m, maxTimes, enc);
    }

    /**
     * 简单get请求
     *
     * @param urlPrefix url前缀
     * @param urlSuffix url后缀
     * @param portName  接口名
     * @return 响应字符串
     */
    public static String get(String urlPrefix, String urlSuffix,
                             String portName, Map<String, String[]> paramMap) {
        return get(urlPrefix, urlSuffix, portName,
                paramMap, null, null, null, null);
    }

    /**
     * 设置表单数据
     *
     * @param m        POST请求对象
     * @param formData 表单数据-参数键值对
     * @param fileMap  文件Map
     */
    private static void setFormDataEntity(HttpPost m, Map<String, String> formData, Map<String, File> fileMap) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        ContentType contentType = ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), Consts.UTF_8);


        if (formData != null) {
            formData.forEach((k, v) -> {
                log.debug("添加Form-Data {} -> {}", k, v);
                builder.addPart(k, new StringBody(v, contentType));
            });
        }
        if (fileMap != null) {
            fileMap.forEach((s, file) -> {
                log.debug("添加文件：{} 文件名：{}", s, file.getName());
                builder.addPart(s, new FileBody(file));
            });
        }

        m.setEntity(builder.build());


    }


    /**
     * 设置header和超时时间
     *
     * @param m       请求对象
     * @param headers header
     * @param timeout 超时时间
     */
    private static void setHeaderConfig(HttpRequestBase m, String cookie, Map<String, String> headers, Integer timeout) {
        timeout = timeout == null ? TIME_OUT : timeout;
        //设置header
        headers.forEach((k, v) -> {
            log.debug("添加header {} -> {}", k, v);
            m.addHeader(k, v);
            m.setHeader(k, v);
        });
        // 设置timeout
        RequestConfig config = RequestConfig.custom()
//                .setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();

        m.setConfig(config);
    }

    /**
     * 发送请求获取相应
     *
     * @param m        请求对象
     * @param maxTimes 最大尝试次数
     * @param enc      响应的编码类型，默认utf-8
     * @return 响应字符串
     */
    private static String executeRequest(HttpRequestBase m, Integer maxTimes, String enc) {
        int times = 0;
        maxTimes = maxTimes == null ? MAX_TIMES : maxTimes;

        long start = System.currentTimeMillis();
        String result = null;
        String timeoutMsg = "请求超时({}) 地址：{}";
        CloseableHttpClient client = HttpClients.createDefault();

        //执行请求
        while (times < maxTimes) {
            try {
                times++;

                log.debug("第{}次请求 地址：{}", times, m.getURI());

                CloseableHttpResponse response = client.execute(m);

                int statusCode = response.getStatusLine().getStatusCode();

                switch (statusCode) {
                    case HttpStatus.SC_BAD_GATEWAY:
                        log.debug("第{}次请求 失败 服务器错误({})", times, statusCode);
                        times--;
                        Thread.sleep(5 * 1000);
                        break;
                    case HttpStatus.SC_MOVED_TEMPORARILY:
                        log.info("第{}次请求 连接被重定向({}) 地址 {}", times, statusCode, response.getStatusLine());
                        times = maxTimes;
                        break;
                    case HttpStatus.SC_OK:
                        long end = System.currentTimeMillis();
                        log.debug("第{}次请求 成功 地址：{} 耗时：{}", times, m.getURI(), formatDuration(end - start));
                        result = EntityUtils.toString(response.getEntity(), enc);
//                        log.debug(result);
                        times = maxTimes;
                        break;
                    case HttpStatus.SC_NOT_FOUND:
                        log.debug("第{}次请求 失败 地址不存在({}) 地址：{} ", times, statusCode, m.getURI());
                        times = maxTimes;
                        break;
                    default:
                        log.info("第{}次请求 未定义错误({})", times, statusCode);
                        times = maxTimes;
                        break;
                }
            } catch (SocketTimeoutException e) {
                if (maxTimes == times) {
                    log.error(timeoutMsg, times, m.getURI());
                } else if ((maxTimes / 3) == times || (maxTimes * 2 / 3) == times) {
                    log.info(timeoutMsg, times, m.getURI());
                } else {
                    log.debug(timeoutMsg, times, m.getURI());
                }
            } catch (IOException e) {
                log.error("请求失败 地址：{}", m.getURI());
            } catch (InterruptedException ignored) {
            }
        }
        return result;
    }

    /**
     * 拼接请求url地址
     *
     * @param urlPrefix url前缀
     * @param urlSuffix url后缀
     * @param portName  接口名
     * @param paramMap  地址栏参数表
     * @param enc       编码类型
     * @return 拼接完成的url地址
     */
    private static String getUrl(String urlPrefix, String urlSuffix,
                                 String portName, Map<String, String[]> paramMap, String enc) {
        urlSuffix = urlSuffix == null ? "" : urlSuffix;
        portName = portName == null ? "" : encode(portName, enc);
        String qs = queryString(paramMap, enc);
        String s = "?";
        String url = urlPrefix + portName + urlSuffix;
        url = (url.contains(s) || "".equals(qs)) ? url : url + s;
        url += qs;
        log.debug("请求地址： " + url);
        return url;
    }

    /**
     * 参数序列化
     *
     * @param paramMap 参数表
     * @return 序列化
     */
    private static String queryString(Map<String, String[]> paramMap, String enc) {
        StringBuilder sb = new StringBuilder();
        if (paramMap == null || paramMap.size() == 0) {
            return "";
        }
        paramMap.forEach((key, value) -> {
            for (String v : value) {
                sb.append("&").append(key).append("=").append(encode(v, enc));
            }
        });
        return sb.toString();
    }

    /**
     * 格式化输出时长 毫秒 秒 分钟
     *
     * @param duration 时长
     * @return 格式化
     */
    private static String formatDuration(long duration) {
        int second = 1000;
        int minute = 60 * second;

        if (duration > minute) {
            return duration / minute + "分钟";
        }
        if (duration > 10 * second) {
            double d = 1.0 * duration / second * 10;
            d = Math.floor(d) / 10;
            return d + "秒";
        }
        return duration + "毫秒";
    }

    /**
     * 解码
     *
     * @param s   待解码字符串
     * @param enc 编码格式 默认utf nga gbk
     * @return 解码完成字符串
     */
    private static String decode(String s, String enc) {
        String encode = null;
        enc = StringUtils.isEmpty(enc) ? "utf-8" : enc;
        try {
            encode = URLDecoder
                    .decode(s, enc);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encode;
    }

    /**
     * 编码
     *
     * @param s   待编码字符串
     * @param enc 编码格式 默认utf nga gbk
     * @return 编码完成字符串
     */
    public static String encode(String s, String enc) {
        String encode = null;
        enc = StringUtils.isEmpty(enc) ? "utf-8" : enc;
        try {
            encode = URLEncoder
                    .encode(s, enc)
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encode;
    }

    /**
     * Unicode编码韩文
     *
     * @param str 待编码字符串
     * @return 编码完成字符串
     */
    private static String unicodeEncode(String str) {
        StringBuffer unicode = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            // 取出每一个字符
            char c = str.charAt(i);
            // 韩文转换为unicode
            if (c >= 44032 && c <= 55215) {
                unicode.append("&#" + Integer.toString(c, 10) + ";");
            } else {
                unicode.append(c);
            }
        }
        return unicode.toString();
    }


    public static void main(String[] args) throws IOException {

    }
}
