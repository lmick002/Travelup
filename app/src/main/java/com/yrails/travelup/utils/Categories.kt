package com.yrails.travelup.utils

import com.yrails.travelup.R

object Categories {
    fun getCategoryImage(category: String?): Int {
        when (category) {
            "한국" -> return R.drawable.category_korea
            "일본" -> return R.drawable.category_japan
            "대만" -> return R.drawable.category_taiwan
            "홍콩 / 마카오" -> return R.drawable.category_hongma
            "싱가포르" -> return R.drawable.category_singapore
            "유럽" -> return R.drawable.category_europe
            "미국 / 캐나다" -> return R.drawable.category_usacanada
            "호주 / 뉴질랜드" -> return R.drawable.category_oceania
            "아시아" -> return R.drawable.category_asia
            "아메리카" -> return R.drawable.category_south_america
            "아프리카" -> return R.drawable.category_africa
        }
        return 0
    }
}