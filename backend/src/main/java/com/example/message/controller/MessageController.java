package com.example.message.controller;

import com.example.common.Result;
import com.example.message.dto.SendMessageDTO;
import com.example.message.entity.Message;
import com.example.message.service.MessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统消息模块控制器
 * <p>
 * 完整请求路径为上下文路径 + 模块前缀，例如发送消息接口：POST /api/message/send
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Api(tags = "系统消息模块接口")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    /** 系统消息模块业务逻辑对象 */
    private final MessageService messageService;

    /**
     * 发送系统消息
     *
     * @param dto 消息入参（接收者ID、消息类型、标题、内容）
     * @return 消息ID
     */
    @ApiOperation(value = "发送系统消息", notes = "需要管理员权限；发送系统通知、公告等消息")
    @PostMapping("/send")
    public Result<Long> sendMessage(@RequestBody @Validated SendMessageDTO dto) {
        return Result.success(messageService.sendMessage(dto));
    }

    /**
     * 获取当前用户的系统消息列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    @ApiOperation(value = "获取消息列表", notes = "获取当前用户的所有系统消息，按时间倒序排列")
    @GetMapping("/list")
    public Result<List<Message>> getMessageList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(messageService.getMessageList(page, size));
    }

    /**
     * 标记消息为已读
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @ApiOperation(value = "标记消息已读", notes = "将指定消息标记为已读状态")
    @PostMapping("/{messageId}/read")
    public Result<Void> markAsRead(@PathVariable Long messageId) {
        messageService.markAsRead(messageId);
        return Result.success();
    }

    /**
     * 标记所有消息为已读
     *
     * @return 操作结果
     */
    @ApiOperation(value = "标记所有消息已读", notes = "将当前用户的所有未读消息标记为已读")
    @PostMapping("/all/read")
    public Result<Void> markAllAsRead() {
        messageService.markAllAsRead();
        return Result.success();
    }

    /**
     * 删除消息
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除消息", notes = "删除指定消息，仅可删除自己的消息")
    @DeleteMapping("/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return Result.success();
    }

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数量
     */
    @ApiOperation(value = "获取未读消息数量", notes = "获取当前用户的未读消息总数")
    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount() {
        return Result.success(messageService.getUnreadCount());
    }
}