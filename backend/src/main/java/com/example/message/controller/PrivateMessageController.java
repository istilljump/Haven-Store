package com.example.message.controller;

import com.example.common.Result;
import com.example.message.dto.SendPrivateMessageDTO;
import com.example.message.service.PrivateMessageService;
import com.example.message.vo.ConversationVO;
import com.example.message.vo.PrivateMessageVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 私信模块控制器（含商品咨询）
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如发送私信：POST /api/message/private/send。
 * 所有接口均需登录，请求方身份一律取自登录态，不接受前端传入的发送者 ID
 *
 * @author ZCode
 * @date 2026/09/25
 */
@Api(tags = "私信模块接口")
@RestController
@RequestMapping("/message/private")
@RequiredArgsConstructor
public class PrivateMessageController {

    /** 私信模块业务逻辑对象 */
    private final PrivateMessageService privateMessageService;

    /**
     * 发送私信 / 发起商品咨询
     *
     * @param dto 发送入参（接收者、私信内容、可选关联商品）
     * @return 新私信 ID
     */
    @ApiOperation(value = "发送私信", notes = "需要登录；带上 productId 即为针对该商品的咨询")
    @PostMapping("/send")
    public Result<Long> send(@RequestBody @Validated SendPrivateMessageDTO dto) {
        return Result.success(privateMessageService.send(dto));
    }

    /**
     * 获取当前用户的会话列表
     *
     * @return 会话列表，按最近一条私信时间倒序
     */
    @ApiOperation(value = "获取会话列表", notes = "需要登录；含对方昵称头像、关联商品、未读条数")
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> listConversations() {
        return Result.success(privateMessageService.listConversations());
    }

    /**
     * 获取与某人的聊天记录
     *
     * @param peerId    对方用户 ID
     * @param productId 关联商品 ID（普通私信不传）
     * @return 按时间正序的聊天记录（调用后对方消息即标记为已读）
     */
    @ApiOperation(value = "获取聊天记录", notes = "需要登录；调用后会把对方发给我的消息标记为已读")
    @GetMapping("/chat")
    public Result<List<PrivateMessageVO>> listChat(
            @RequestParam Long peerId,
            @RequestParam(required = false) Long productId) {
        return Result.success(privateMessageService.listChat(peerId, productId));
    }

    /**
     * 获取未读私信总数
     *
     * @return 未读条数
     */
    @ApiOperation(value = "获取未读私信总数", notes = "需要登录；用于头部消息角标")
    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount() {
        return Result.success(privateMessageService.getUnreadCount());
    }

    /**
     * 删除会话
     *
     * @param peerId    对方用户 ID
     * @param productId 关联商品 ID（普通私信不传）
     * @return 操作结果
     */
    @ApiOperation(value = "删除会话", notes = "需要登录；删除后会话双方均不再可见该会话")
    @DeleteMapping("/chat")
    public Result<Void> deleteConversation(
            @RequestParam Long peerId,
            @RequestParam(required = false) Long productId) {
        privateMessageService.deleteConversation(peerId, productId);
        return Result.success();
    }
}
