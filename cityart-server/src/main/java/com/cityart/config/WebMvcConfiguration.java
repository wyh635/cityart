package com.cityart.config;

import com.cityart.interceptor.JwtTokenUserInterceptor;
import com.cityart.json.JacksonObjectMapper;
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

import java.util.List;

/**
 * 配置类，注册 web 层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

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
                        "/api/app/dict/cities"
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
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
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
