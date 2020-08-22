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
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/*
        <!--json解析-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.47</version>
        </dependency>

        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpmime</artifactId>
            <version>4.5.3</version>
        </dependency>
 */


/**
 * 请求工具类
 *
 * @author bx002
 */
@Slf4j
public class RequestUtil {
    private final static String SLASH = "/";
    private final static String PARAM_PREFIX = "?";
    /**
     * 默认最大尝试次数
     */
    final static int DEFAULT_MAX_TIMES = 10;
    final static int DEFAULT_TIME_OUT = 15 * 1000;
    /**
     * 默认请求头
     */
    private final static Map<String, String> DEFAULT_HEADERS = new HashMap<>();

    public static Map<String, String> getDefaultHeaders() {
        return DEFAULT_HEADERS;
    }

    static {
        DEFAULT_HEADERS.put("Content-Type", "application/json");
        DEFAULT_HEADERS.put("Accept-Language", "zh-CN,zh;q=0.9");
    }

    /**
     * post请求
     *
     * @param url      请求地址
     * @param inter    接口
     * @param paramMap 地址栏参数
     * @param formData 表单数据
     * @param cookie   cookie
     * @param maxTimes 最大尝试次数
     * @param timeout  超时时间
     * @return 请求结果
     */
    public static String post(String url, String inter, Map<String, String[]> paramMap, Map<String, String> formData,
                              String cookie, Integer maxTimes, Integer timeout, String enc) {
        String urlString = getUrl(url, inter, paramMap, enc);
        HttpPost request = new HttpPost(urlString);
        Map<String, String> headers;

        if (formData != null) {
            headers = new HashMap<>();
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            //放入formData
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            ContentType contentType = ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), Consts.UTF_8);

            formData.forEach((k, v) -> {
                log.debug("添加Form-Data {} -> {}", k, v);
                builder.addPart(k, new StringBody(v, contentType));
            });

            request.setEntity(builder.build());

        } else {
            headers = getDefaultHeaders();
        }
        headers.put("cookie", cookie);

        return request(request, maxTimes, timeout, headers, enc);
    }

    public static String post(String url, String inter, Map<String, String[]> paramMap, Map<String, String> formData,
                              String cookie, String enc) {
        return post(url, inter, paramMap, formData, cookie, null, null, enc);
    }

    /**
     * get请求
     *
     * @param url      地址
     * @param inter    接口
     * @param paramMap 地址栏参数
     * @param cookie   cookie
     * @param maxTimes 最大尝试次数
     * @param timeout  超时时间
     * @return 请求结果
     */
    public static String get(String url, String inter, Map<String, String[]> paramMap,
                             String cookie, Integer maxTimes, Integer timeout, String enc) {
        String urlString = getUrl(url, inter, paramMap, enc);
        HttpGet request = new HttpGet(urlString);
        Map<String, String> headers;

        headers = getDefaultHeaders();
        headers.put("cookie", cookie);

        return request(request, maxTimes, timeout, headers, enc);
    }

    public static String get(String url, String inter, Map<String, String[]> paramMap,
                             String cookie, String enc) {
        return get(url, inter, paramMap, cookie, null, null, enc);
    }

    /**
     * 请求
     *
     * @param m        POST/GET方法
     * @param maxTimes 最大尝试次数
     * @return
     */
    private static String request(HttpRequestBase m, Integer maxTimes, Integer timeout, Map<String, String> headers, String enc) {
        maxTimes = maxTimes != null ? maxTimes : DEFAULT_MAX_TIMES;
        timeout = timeout != null ? timeout : DEFAULT_TIME_OUT;
        String result = null;
        String timeoutMsg = "请求超时({}) 地址：{}";
        int times = 0;

        CloseableHttpClient client = HttpClients.createDefault();

        long start = System.currentTimeMillis();

        //设置header
        headers = headers != null ? headers : DEFAULT_HEADERS;

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
                        log.info("第{}次请求 连接被重定向({})", times, statusCode);
                        times = maxTimes;
                        break;
                    case HttpStatus.SC_OK:
                        long end = System.currentTimeMillis();
                        log.debug("第{}次请求 成功 地址：{} 耗时：{}", times, m.getURI(), formatDuration(end - start));
                        result = EntityUtils.toString(response.getEntity(), enc);
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
                if (maxTimes.equals(times)) {
                    log.error(timeoutMsg, times, m.getURI());
                } else if ((maxTimes / 3) == times || (maxTimes * 2 / 3) == times) {
                    log.info(timeoutMsg, times, m.getURI());
                } else {
                    log.debug(timeoutMsg, times, m.getURI());
                }
            } catch (IOException e) {
                log.error("请求失败 地址：{}", m.getURI());
//                e.printStackTrace();
            } catch (InterruptedException ignored) {
            }
        }


        m.releaseConnection();
        return result;
    }


    /**
     * 拼接请求地址
     *
     * @param url      url
     * @param inter    接口名，如无留空
     * @param paramMap 地址栏传参
     * @return 拼接好的url
     */
    private static String getUrl(String url, String inter, Map<String, String[]> paramMap, String enc) {
        String qs = queryString(paramMap, enc);
        inter = inter != null ? encode(inter, enc) : "";
        inter = (inter.contains(PARAM_PREFIX) || "".equals(qs)) ? inter : inter + PARAM_PREFIX;
//        url = url.endsWith(SLASH) ? url : url + SLASH;
        String s = url + inter + qs;
        log.debug("请求地址： " + s);
        return s;
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
     * 8编码
     *
     * @param s 待编码字符串
     * @return 编码完成字符串
     */
    public static String encode(String s, String enc) {
        String encode = null;
        try {
            encode = URLEncoder
//                    .encode(s, "gbk")
                    .encode(s, StringUtils.isEmpty(enc) ? "utf-8" : enc)
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encode;
    }


    /**
     * utf-8解码
     *
     * @param s 待编码字符串
     * @return 编码完成字符串
     */
    public static String decode(String s) {
        String encode = null;
        try {
            encode = URLDecoder
                    .decode(s, "gbk");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encode;
    }

//    public static void main(String[] args) {
//        String html = get("https://m.weibo.cn/status/4540412688340564", null, null, null, "utf-8");
//
//        html = html.substring(html.indexOf("\"text\": \"<a  href=\\\"") + 9);
//        html = html.substring(0, html.indexOf("\","));
//
//        html = replaceLinks(html);
//
//        System.err.println(html);
//    }
}
