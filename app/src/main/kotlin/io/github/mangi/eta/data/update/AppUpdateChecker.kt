package io.github.mangi.eta.data.update

import io.github.mangi.eta.BuildConfig
import io.github.mangi.eta.R
import io.github.mangi.eta.core.AppStrings
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** GitHub 最新 Release 信息，[version] 已去掉 tag 的 v 前缀。 */
internal data class AppLatestRelease(
    val version: String,
    val url: String,
)

/**
 * 应用更新检查：读取 GitHub 最新 Release 并与当前版本比较。
 *
 * 版本号遵循发版流程的 v 前缀 + 点分数字约定（v3.0.6），比较按段进行：两段都是数字时
 * 按数值比较，否则按字符串比较。上游版本相同时比较 fork 日期与修订号；构建类型后缀（-debug）
 * 与 CI 构建元数据（+提交号）不参与比较。
 * 所有网络与响应错误统一抛 [IOException]，由调用方提示。
 */
internal object AppUpdateChecker {

    fun fetchLatest(releaseUrl: String = latestReleaseUrl()): AppLatestRelease {
        val request = Request.Builder()
            .url(releaseUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException(
                    AppStrings.get(
                        R.string.update_err_github_release_http,
                        "GitHub Release 请求失败（HTTP %1\$d）",
                        response.code,
                    ),
                )
            }
            val json = runCatching { JSONObject(response.body.string()) }
                .getOrElse {
                    throw IOException(
                        AppStrings.get(
                            R.string.update_err_github_release_invalid_json,
                            "GitHub Release 响应不是有效 JSON",
                        ),
                        it,
                    )
                }
            val tag = json.optString("tag_name").trim()
            val url = json.optString("html_url").trim()
            if (tag.isEmpty() || url.isEmpty()) {
                throw IOException(
                    AppStrings.get(
                        R.string.update_err_github_release_missing_fields,
                        "GitHub Release 响应缺少 tag_name/html_url",
                    ),
                )
            }
            return AppLatestRelease(version = tag.removePrefix("v"), url = url)
        }
    }

    fun isNewer(latest: String, current: String): Boolean {
        val latestVersion = latest.removePrefix("v").substringBefore('+').removeSuffix("-debug")
        val currentVersion = current.removePrefix("v").substringBefore('+').removeSuffix("-debug")
        val compared = compareParts(
            latestVersion.substringBefore('-').split('.'),
            currentVersion.substringBefore('-').split('.'),
        )
        if (compared != 0) return compared > 0
        // 上游版本相同时再比较 fork 的日期与当日修订号；首包等价于修订 0。
        return compareParts(forkParts(latestVersion), forkParts(currentVersion)) > 0
    }

    private fun forkParts(version: String): List<String> =
        version.substringAfter("-fork.", "").takeIf { it.isNotEmpty() }?.split('.').orEmpty()

    private fun compareParts(latest: List<String>, current: List<String>): Int {
        for (index in 0 until maxOf(latest.size, current.size)) {
            val latestPart = latest.getOrNull(index) ?: "0"
            val currentPart = current.getOrNull(index) ?: "0"
            val latestNumber = latestPart.toLongOrNull()
            val currentNumber = currentPart.toLongOrNull()
            val compared = if (latestNumber != null && currentNumber != null) {
                latestNumber.compareTo(currentNumber)
            } else {
                latestPart.compareTo(currentPart)
            }
            if (compared != 0) return compared
        }
        return 0
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    internal fun latestReleaseUrl(repository: String = BuildConfig.UPDATE_REPOSITORY): String =
        "https://api.github.com/repos/$repository/releases/latest"
    private const val USER_AGENT = "Eta-Update-Checker"
}
