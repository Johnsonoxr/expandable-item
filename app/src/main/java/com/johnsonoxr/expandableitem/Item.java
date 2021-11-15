package com.johnsonoxr.expandableitem;


import android.graphics.Color;

import java.util.Calendar;
import java.util.Random;

import androidx.core.graphics.ColorUtils;
public class Item {
    private static final Random sRandom = new Random();

    public final int color = ColorUtils.blendARGB(sRandom.nextInt(), Color.BLACK, .7f);
    public final long date = Math.abs(sRandom.nextLong()) % Calendar.getInstance().getTimeInMillis();

    public boolean selected = false;
}
