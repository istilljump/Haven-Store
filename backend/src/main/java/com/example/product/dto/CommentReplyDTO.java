package com.example.product.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 卖家回评入参（轮8 业务迭代新增）
 *
 * @author ZCode
 * @date 2026/11
 */
@Data
public class CommentReplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 回评内容（每条评价仅一条回评，≤500 字） */
    @NotBlank(message = "回评内容不能为空")
    @Size(max = 500, message = "回评内容最多 500 字")
    private String reply;
}
