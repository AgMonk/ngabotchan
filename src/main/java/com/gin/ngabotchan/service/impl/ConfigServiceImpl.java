package com.gin.ngabotchan.service.impl;

import com.gin.ngabotchan.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Map;

/**
 * @author bx002
 */
@Slf4j
@Service
public class ConfigServiceImpl implements ConfigService {
    /**
     * 发帖使用的账号cookie
     */
    final static File COOKIES_TEST = new File("d:/cookie.txt");
    final static File COOKIES = new File("cookie.txt");
    /**
     * 预设的版面id
     */
    final static File FID = new File("fid.txt");
    /**
     * 回复的帖子id
     */
    final static File TID = new File("tid.txt");


    public ConfigServiceImpl() {
        String createFail = "文件创建失败";
        if (!COOKIES.exists()) {
            try {
                log.info("cookie文件未找到，已创建，请填写账号cookie");
                log.info("格式： {自定义名称}:{cookie}  例如： 银之石:asdqioyonjk  注意冒号为英文，每行一个");
                while (!COOKIES.createNewFile()) {
                    log.error(createFail);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!FID.exists()) {
            try {
                log.info("fid文件未找到，已创建默认文件，请按照格式添加自定义版面，每行一个");
                while (!FID.createNewFile()) {
                    log.error(createFail);
                }
                PrintWriter pw = new PrintWriter(FID);
                pw.println("少女前线:-547859");
                pw.flush();
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!TID.exists()) {
            try {
                log.info("tid文件未找到，已创建默认文件，请按照格式添加帖子id，每行一个");
                while (!TID.createNewFile()) {
                    log.error(createFail);
                }
                PrintWriter pw = new PrintWriter(TID);
                pw.println("少前水楼:15666793");
                pw.flush();
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        loadConfig(COOKIES, COOKIE_MAP);
        loadConfig(COOKIES_TEST, COOKIE_MAP);
        loadConfig(FID, FID_MAP);
        loadConfig(TID, TID_MAP);
    }


    /**
     * 从文件中读取配置信息写入map
     */
    private static void loadConfig(File file, Map<String, String> map) {
        if (!file.exists()) {
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                log.info("读取: " + line);
                String key = line.substring(0, line.indexOf(":"));
                String value = line.substring(line.indexOf(":") + 1);
                map.put(key, value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
