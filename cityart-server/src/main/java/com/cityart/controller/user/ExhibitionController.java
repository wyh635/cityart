package com.cityart.controller.user;

import com.cityart.dto.ExhibitionPageQueryDTO;
import com.cityart.result.PageResult;
import com.cityart.result.Result;
import com.cityart.service.ExhibitionService;
import com.cityart.vo.ExhibitionPageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端「全部展览」接口（免登录）
 * GET /api/app/exhibitions/page
 */
@RestController
@RequestMapping("/api/app/exhibitions")
@Api(tags = "C端全部展览接口")
@Slf4j
public class ExhibitionController {

    @Autowired
    private ExhibitionService exhibitionService;

    /**
     * 全部展览分页列表
     * 「正在展出 / 即将展出 / 往期展出」三个区块共用本接口，用 status 区分：
     * status=current&pageSize=2 → 正在展出区块；status=future / past 同理
     * 参数校验注解写在 ExhibitionPageQueryDTO 的字段上，@Validated 让它生效
     */
    @GetMapping("/page")
    @ApiOperation("全部展览分页列表")
    public Result<PageResult<ExhibitionPageVO>> page(@Validated ExhibitionPageQueryDTO dto) {
        log.info("查询全部展览，status：{}，type：{}，city：{}，keyword：{}，sort：{}，page：{}/{}",
                dto.getStatus(), dto.getType(), dto.getCity(), dto.getKeyword(),
                dto.getSort(), dto.getPage(), dto.getPageSize());
        return Result.success(exhibitionService.pageQuery(dto));
    }
}
