package com.cityart.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String SUCCESS = "操作成功";
    public static final String SYSTEM_ERROR = "系统异常";
    public static final String PARAM_ERROR = "参数错误";
    public static final String RECOMMEND_SERVICE_ERROR = "推荐服务暂时不可用";
    public static final String EXHIBITION_NOT_FOUND = "展览不存在";
    public static final String GALLERY_NOT_FOUND = "美术馆不存在";
    // ===== 票务与个人中心模块 =====
    public static final String TICKET_TYPE_NOT_FOUND = "票种不存在或已停售";
    public static final String VISIT_DATE_INVALID = "所选日期不可预约";
    public static final String CALENDAR_SOLD_OUT = "所选日期已售完";
    public static final String STOCK_NOT_ENOUGH = "库存不足";
    /** 限购文案带数量，用 String.format(ORDER_QUANTITY_LIMIT, limitPerUser) 拼 */
    public static final String ORDER_QUANTITY_LIMIT = "超出限购数量，每人最多可购 %d 张";
    public static final String ORDER_PAY_EXPIRED = "订单已超时，请重新下单";
    public static final String ORDER_NOT_BELONG_TO_USER = "无权操作该订单";
    public static final String REFUND_ALREADY_USED = "已核销的订单不可退款";
    public static final String REFUND_EXHIBITION_ENDED = "展览已结束，不可退款";
    public static final String REFUND_PROCESSING = "退款申请处理中";
    // ===== 扫码核销模块（B 端） =====
    public static final String VERIFY_NOT_PAID = "该订单未支付，不能核销";
    public static final String VERIFY_ALREADY_USED = "该电子票已核销";
    public static final String VERIFY_TOO_EARLY = "未到观展日期，不能核销";
    public static final String VERIFY_EXPIRED = "电子票已过期，不能核销";
    public static final String VERIFY_PARAM_MISSING = "请提供二维码或订单号";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";

}
