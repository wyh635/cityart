package com.cityart.controller.admin;

import com.cityart.result.Result;
import com.cityart.service.DataRepairService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * B 端「数据校准」接口
 * <p>
 * 冗余字段（已售数量 / 在展数量 / 观展次数等）每天凌晨有定时任务兜底校准，
 * 这个接口给运营一个**手工立即校准**的入口 ——
 * 例如刚导入了一批历史订单，不想等到凌晨。
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "B端数据校准接口")
@Slf4j
public class AdminDataRepairController {

    @Autowired
    private DataRepairService dataRepairService;

    /**
     * 立即校准全部冗余字段
     * <p>
     * 返回各字段**实际被修正的行数**；全为 0 表示数据本来就一致。
     */
    @PostMapping("/data-repair")
    @ApiOperation("立即校准冗余字段")
    public Result<Map<String, Integer>> repair() {
        log.info("后台手动触发数据校准");
        return Result.success("校准完成", dataRepairService.repairAll());
    }
}
