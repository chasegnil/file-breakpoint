package com.gnil.filebreakpoint.controller;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.NumberFormat;

/**
 * 参考：https://blog.csdn.net/A1032453509/article/details/78045957
 */
@Controller
@EnableAutoConfiguration
public class Main {

    @RequestMapping("/")
    public void home(HttpServletRequest request, HttpServletResponse response) {
        String range = request.getHeader("range");

        File file = new File("F:\\KuGou\\陈少华 - 九月九的酒.mp3");

        // 开始下载位置
        long startByte = 0;
        // 结束下载位置
        long endByte = file.length() - 1;

        // 有range的话
        if (range != null && range.contains("bytes=") && range.contains("-")) {
            range = range.substring(range.lastIndexOf("=") + 1).trim();
            String[] ranges = range.split("-");
            try {
                // 判断range的类型
                if (ranges.length == 1) {
                    // 类型一：bytes=-2341
                    if (range.startsWith("-")) {
                        endByte = Long.parseLong(ranges[0]);
                    }
                    //类型二：bytes=2343-
                    else if (range.endsWith("-")) {
                        startByte = Long.parseLong(ranges[0]);
                    }
                }
                // 类型三：bytes=22-2343
                else if (ranges.length == 2) {
                    startByte = Long.parseLong(ranges[0]);
                    endByte = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                startByte = 0;
                endByte = file.length() - 1;
            }
        }

        //要下载的长度
        long contentLength = endByte - startByte + 1;
        // 文件名
        String fileName = file.getName();
        // 文件类型
        String contentType = request.getServletContext().getMimeType(fileName);

        //各种响应头设置
        //参考资料：https://www.ibm.com/developerworks/cn/java/joy-down/index.html
        response.setHeader("Accept-Ranges", "bytes");
        // http状态码要为206
        response.setStatus(response.SC_PARTIAL_CONTENT);
        response.setContentType(contentType);
        response.setHeader("Content-Type", contentType);
        //这里文件名换你想要的，inline表示浏览器直接实用
        //参考资料：http://hw1287789687.iteye.com/blog/2188500
        response.setHeader("Content-Disposition", "inline;filename=" + fileName);
        response.setHeader("Content-Length", String.valueOf(contentLength));
        // Content-Range，格式为[要下载的开始位置]-[结束位置]/[文件总大小]
        response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + file.length());

        BufferedOutputStream outputStream = null;
        RandomAccessFile randomAccessFile = null;
        // 已传送数据大小
        long transmitted = 0;
        try {
            randomAccessFile = new RandomAccessFile(file,"r");// 读权限
            outputStream = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[4096];
            int len = 0;
            randomAccessFile.seek(startByte);
            while ((transmitted + len) <= contentLength && (len = randomAccessFile.read(buff)) != -1) {
                outputStream.write(buff, 0, len);
                transmitted += len;
            }
            //处理不足buff.length部分
            if (transmitted < contentLength) {
                len = randomAccessFile.read(buff, 0, (int)(contentLength - transmitted));
                outputStream.write(buff,0, len);
                transmitted += len;
            }

            outputStream.flush();
            response.flushBuffer();
            randomAccessFile.close();
            System.out.println("下载完毕：" + startByte + "-" + endByte + "：" + transmitted);
        } catch (ClientAbortException e) {
            System.out.println("用户停止下载：" + startByte + "-" + endByte + "：" + transmitted);
            //捕获此异常表示拥护停止下载
        } catch (IOException e) {

        } finally {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {

            }
        }
    }
}
