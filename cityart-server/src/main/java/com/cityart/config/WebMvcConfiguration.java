package com.cityart.config;

import com.cityart.interceptor.JwtTokenAdminInterceptor;
import com.cityart.interceptor.JwtTokenUserInterceptor;
import com.cityart.json.JacksonObjectMapper;
import com.cityart.properties.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.io.File;
import java.util.List;

/**
 * 配置类，注册 web 层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private UploadProperties uploadProperties;

    /**
     * 注册自定义拦截器
     * <p>
     * 注意：addPathPatterns 的路径必须和 Controller 上的 @RequestMapping 前缀一致，
     * 否则拦截器不会触发（本项目 C 端 Controller 前缀统一为 /api/app）。
     */
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");

        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/api/app/**")
                .excludePathPatterns(
                        // 登录、注册发生在拿到 token 之前，必须放行
                        "/api/app/login",
                        "/api/app/register",
                        // 首页 4 个接口都是免登录（🔓）的，必须显式放行，否则会被拦截返回 401
                        "/api/app/banner",
                        "/api/app/exhibition",
                        "/api/app/recommend",
                        "/api/app/gallery",
                        // 「全部展览 / 美术馆列表」模块的 5 个接口同样免登录，必须显式放行
                        "/api/app/exhibitions/page",
                        "/api/app/galleries/page",
                        "/api/app/dict/exhibition-types",
                        "/api/app/dict/gallery-types",
                        "/api/app/dict/cities",
                        // 「展览作品集 / 馆内展览列表」是纯查询，游客可看，直接放行
                        "/api/app/exhibitions/*/works",
                        "/api/app/galleries/*/exhibitions",
                        // ⚠️ 注意：/api/app/galleries/* 和 /api/app/exhibitions/* 已从这里移除。
                        //    详情接口要返回「当前用户是否已收藏」，必须过拦截器才能拿到 userId，
                        //    所以改用 @OptionalLogin 注解（无 token 按游客放行，有 token 解析出 userId），
                        //    写在 ExhibitionController#detail 和 GalleryController#detail 上。
                        // 票务模块里只有这三个是免登录的（票种、观展日历、支付回调）
                        // ⚠️ 订单 / 票夹 / 看展记录 / 收藏 / 个人中心一律不放行，必须带 token
                        "/api/app/exhibitions/*/ticket-types",
                        "/api/app/exhibitions/*/calendar",
                        "/api/app/orders/pay-notify"
                );

        // ==================== B 端管理员拦截器 ====================
        // 与 C 端是两套独立体系：不同路径、不同密钥、不同 claims 键、不同 ThreadLocal
        // C 端 token 拿到这里验不过（密钥不同）→ 401
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        // 登录发生在拿到 token 之前，必须放行
                        "/api/admin/login"
                );
    }

    /**
     * 通过 knife4j 生成接口文档，访问地址：http://localhost:8080/doc.html
     */
    @Bean
    public Docket docket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("cityart项目接口文档")
                .version("2.0")
                .description("cityart项目接口文档")
                .build();
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("用户端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.cityart.controller.user"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 设置静态资源映射
     * <p>
     * ⚠️ /static/upload/** 这条是**上传文件的访问入口**：
     * 图片落盘在 cityart.upload.base-path（默认 ./uploads），
     * 通过 /static/upload/** 对外暴露，DB 里存的 URL 就是 /static/upload/xxx/yyy.jpg。
     * <p>
     * 注意：这里用的是**绝对路径**（file: 前缀 + 解析后的绝对路径），
     * 因为 Tomcat 的工作目录不确定，相对路径会随启动方式变化导致图片 404。
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");

        // 上传文件的物理目录 → URL 映射
        // ⚠️ 必须与 UploadServiceImpl 解析出的目录一致：
        //    两边都用 new File(basePath).getAbsoluteFile() 逐字对应，
        //    否则会出现「上传成功但图片 404」
        File uploadDir = new File(uploadProperties.getBasePath()).getAbsoluteFile();
        String location = "file:" + uploadDir.getPath() + File.separator;
        registry.addResourceHandler(uploadProperties.getUrlPrefix() + "/**")
                .addResourceLocations(location);
        log.info("上传文件静态映射：{}/** → {}", uploadProperties.getUrlPrefix(), location);
    }

    /**
     * 扩展 Spring MVC 框架的消息转化器：统一日期序列化格式（yyyy-MM-dd HH:mm）
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new JacksonObjectMapper());
        // 放到第 0 位，保证优先使用
        converters.add(0, converter);
    }
}
