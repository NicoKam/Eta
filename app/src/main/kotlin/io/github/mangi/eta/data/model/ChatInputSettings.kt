package io.github.mangi.eta.data.model

import kotlinx.serialization.Serializable

/**
 * 对话输入框行为配置。
 *
 * [sendOnEnter] 为 true 时输入框以 `ImeAction.Send` 标记 IME：软键盘把回车键渲染为发送，
 * 物理键盘回车也直接提交草稿；Shift+回车仍插入换行。关闭时回落到 `ImeAction.Default`，
 * 回车只做换行，与 3.4.2 及更早版本一致。
 */
@Serializable
internal data class ChatInputSettings(
    val sendOnEnter: Boolean = false,
)
