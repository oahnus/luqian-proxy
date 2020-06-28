package com.github.oahnus.proxyserver.utils;

import com.github.oahnus.proxyserver.entity.SysDomain;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oahnus on 2020-06-22
 */
public class NginxUtils {
    public static void generateNginxServerConfig(SysDomain sysDomain, OutputStream out) throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_20);
        cfg.setDirectoryForTemplateLoading(new File(NginxUtils.class.getClassLoader().getResource("").getFile()));
        cfg.setDefaultEncoding("utf8");
        OutputStreamWriter writer = new OutputStreamWriter(out, "utf8");

        Map<String, Object> params = new HashMap<>();
        params.put("domain", sysDomain.getDomain());
        params.put("port", sysDomain.getPort());
        params.put("https", sysDomain.getHttps());

        Template template = cfg.getTemplate("server.tpl");
        template.process(params, writer);
        writer.flush();
        writer.close();
    }
    public static String generateNginxServerConfig(SysDomain sysDomain) throws IOException, TemplateException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generateNginxServerConfig(sysDomain, out);
        return out.toString();
    }

    public static void main(String... args) throws IOException, TemplateException {
        SysDomain testDomain = new SysDomain();
        testDomain.setHttps(true);
        testDomain.setDomain("c323d.proxy.oahnus.top");
        testDomain.setPort(30006);

        System.out.println(generateNginxServerConfig(testDomain));
    }
}
