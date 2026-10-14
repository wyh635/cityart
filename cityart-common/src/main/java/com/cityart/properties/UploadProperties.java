package com.cityart.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置（对应 application.yml 里的 cityart.upload.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "cityart.upload")
public class UploadProperties {

    /**
     * 上传文件的**物理存储根目录**（相对路径会解析成「进程工作目录」下的子目录）
     * <p>
     * ⚠️ 故意放在 classpath / target 之外：打 jar 后 classpath 不可写，
     * 而且重新打包会把上传的图片一起冲掉。
     * <p>
     * 📌 注意「相对路径」的基准是**进程工作目录**，不是项目根目录：
     * <ul>
     *     <li>IDEA 里直接跑 {@code CityartApplication} → 工作目录是 cityart-server/，落在 cityart-server/uploads</li>
     *     <li>{@code mvn -pl cityart-server spring-boot:run} → 同上</li>
     *     <li>生产 {@code java -jar} → 取决于启动命令所在目录</li>
     * </ul>
     * 想固定位置就配成绝对路径，例如 {@code base-path: D:/cityart/uploads}。
     */
    private String basePath = "./uploads";

    /**
     * 对外访问的 URL 前缀
     * <p>
     * 与 WebMvcConfiguration 里注册的资源映射保持一致：
     * /static/upload/** → file:{basePath}/
     */
    private String urlPrefix = "/static/upload";
}
