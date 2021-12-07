package com.audiobook.utils;

import android.content.Context;

/**
 * @author 优雅永不过时
 * @Package com.audiobook.utils
 * @Date 2021/10/25 23:55
 */
public class SizeUtil {
    public static int dip2px(Context context, double dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5);
    }

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
