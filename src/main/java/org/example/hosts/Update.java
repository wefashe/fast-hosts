package org.example.hosts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Update {
    public static void main(String[] args) throws Exception {
        String[] githubHosts = {
                "alive.github.com",
                "live.github.com",
                "github.githubassets.com",
                "central.github.com",
                "desktop.githubusercontent.com",
                "assets-cdn.github.com",
                "camo.githubusercontent.com",
                "github.map.fastly.net",
                "github.global.ssl.fastly.net",
                "gist.github.com",
                "github.io",
                "github.com",
                "github.blog",
                "api.github.com",
                "raw.githubusercontent.com",
                "user-images.githubusercontent.com",
                "favicons.githubusercontent.com",
                "avatars5.githubusercontent.com",
                "avatars4.githubusercontent.com",
                "avatars3.githubusercontent.com",
                "avatars2.githubusercontent.com",
                "avatars1.githubusercontent.com",
                "avatars0.githubusercontent.com",
                "avatars.githubusercontent.com",
                "codeload.github.com",
                "github-cloud.s3.amazonaws.com",
                "github-com.s3.amazonaws.com",
                "github-production-release-asset-2e65be.s3.amazonaws.com",
                "github-production-user-asset-6210df.s3.amazonaws.com",
                "github-production-repository-file-5c1aeb.s3.amazonaws.com",
                "githubstatus.com",
                "github.community",
                "github.dev",
                "collector.github.com",
                "pipelines.actions.githubusercontent.com",
                "media.githubusercontent.com",
                "cloud.githubusercontent.com",
                "objects.githubusercontent.com",
                "vscode.dev",
        };

        int threadSize = Math.min(githubHosts.length, Runtime.getRuntime().availableProcessors() * 2 + 1);
        ExecutorService es = Executors.newFixedThreadPool(threadSize);
        List<Future<String>> results = new LinkedList<Future<String>>();
        for (String host : githubHosts) {
            Future<String> future = es.submit(() -> findWrapper(host));
            results.add(future);
        }
        // 执行子线程
        es.shutdown();
        try {
            while (!es.awaitTermination(100, TimeUnit.MINUTES)) {
                // 超时的时候向线程池中所有的线程发出中断
                es.shutdownNow();
            }
        } catch (InterruptedException e) {
            es.shutdownNow();
        }

        StringBuilder hostsString = new StringBuilder();
        String text = "# GitHub Host Start";
        hostsString.append(text).append(System.lineSeparator());
        for (Future<String> result : results) {
            text = result.get();
            hostsString.append(text).append(System.lineSeparator());
        }

        hostsString.append(System.lineSeparator());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC+8"));
        DateTimeFormatter ofPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        String dateTime = now.format(ofPattern);
        String week = getWeek(now);
        String time = String.format("%s %s", dateTime, week);
        text = String.format("# update time: %s", time);
        hostsString.append(text).append(System.lineSeparator());
        text = "# GitHub Host End";
        hostsString.append(text).append(System.lineSeparator());

        StringBuilder context = new StringBuilder();
        context.append("数据更新时间：").append(time).append(System.lineSeparator());
        context.append("```").append(System.lineSeparator());
        context.append(hostsString);
        context.append("```").append(System.lineSeparator());

        Path readme = Paths.get("README.md");
        Path hosts = Paths.get("hosts");
        if (!Files.exists(readme)) {
            Files.createFile(readme);
        }
        if (!Files.exists(hosts)) {
            Files.createFile(hosts);
        }
        Files.write(hosts, hostsString.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(readme, context.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String getWeek(LocalDateTime dateTime){
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        switch (dayOfWeek) {
            case MONDAY:
                return "星期一";
            case TUESDAY:
                return "星期二";
            case WEDNESDAY:
                return "星期三";
            case THURSDAY:
                return "星期四";
            case FRIDAY:
                return "星期五";
            case SATURDAY:
                return "星期六";
            case SUNDAY:
                return "星期日";
            default:
                return "未知";
        }
    }

    public static String findWrapper(String host) throws Exception {
        String ip = findIp(host);
        return String.format("%-15s     %s", ip, host);
    }

    // public static String findIp(String host) throws Exception {
    //     String url = resolveUrl(host);
    //     Element body = Jsoup.connect(url)
    //             .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36")
    //             .timeout(20000)
    //             .referrer("https://www.ipaddress.com/")
    //             .get()
    //             .body();
    //     String text = body.select("#dnsinfo > tr:nth-child(1) > td:nth-child(3) > a").text();
    //     if (text == null || text.equals("")) {
    //         text = body.select("#dnsinfo > tr:nth-child(2) > td:nth-child(3) > a").text();
    //     }
    //     System.out.println(text + "  " + host);
    //     return text;
    // }

    public static String findIp(String host) throws Exception {
        String url = String.format("https://sitereport.netcraft.com/?url=%s", host);
        Element body = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36")
                .timeout(20000)
                .referrer("https://sitereport.netcraft.com/")
                .get()
                .body();
        String text = body.select("#ip_address").text();
        System.out.printf("正在为[%s]选取最快的IP...\n", host);
        System.out.println(text + "  " + host);
        return text;
    }

    public static String resolveUrl(String url) {
        String ipAddressFooter = ".ipaddress.com";
        String[] urlBody = url.split("\\.");
        if (urlBody.length > 2) {
            return "https://" + urlBody[urlBody.length - 2] + '.' + urlBody[urlBody.length - 1] + ipAddressFooter + "/" + url;
        }
        return "https://" + url + ipAddressFooter;
    }

    public static long ping(String ip) throws Exception{
        long start = System.currentTimeMillis();
        boolean status = Runtime.getRuntime().exec("ping -c 1 "+ ip).waitFor(1L, TimeUnit.SECONDS);
        long stop = System.currentTimeMillis();
        if(!status){
            return -1;
        }else{
            return stop - start;
        }
    }
}
