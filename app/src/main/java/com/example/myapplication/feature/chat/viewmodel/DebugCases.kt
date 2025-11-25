package com.example.myapplication.feature.chat.viewmodel

/**
 * A container for debug conversation sequences.
 * You can add more cases here for testing.
 */
object DebugCases {
    val modelName = "qwen-plus"
    /**
     * Case 1: Role-playing and letter writing.
     */
    val case1 = listOf(
        "现在你叫小明在读大二学生",
        "简单介绍下你自己",
        "写一封信200字的信给李华让他来参加聚会"
    )

    /**
     * Case 2: A placeholder for another test sequence.
     * You can manually edit this to create a new test case.
     */
    val case2 = listOf(
        "你好，你是什么模型？",
        "请用Python写一个快速排序算法。"
    )
    val case3 = listOf(
        "输出一下\"![](/storage/emulated/0/Pictures/f338830b-4a81-46da-a82a-8ca1a79c2d89.jpg)\"这个字符串，不用多余输出"
    )
    // You can add more cases like case3, case4, etc.
}
