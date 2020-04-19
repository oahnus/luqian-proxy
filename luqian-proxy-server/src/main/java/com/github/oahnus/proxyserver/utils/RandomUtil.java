package com.github.oahnus.proxyserver.utils;

import java.util.Random;

/**
 * Created by oahnus on 2020-02-28
 * 17:52.
 */
public class RandomUtil {
    private static Random random = new Random();
    private static final String SEQ_1 = "abcdefghijklmnopqrstuvwxyz";
    private static final String SEQ_2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SEQ_3 = "1234567890";

    // 去除易混淆字符 i l o
    // _S short
    private static final String SEQ_1_S = "abcdefghjkmnpqrstuvwxyz";
    // 去除易混淆字符 I L O
    private static final String SEQ_2_S = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    // 去除易混淆字符 1 0
    private static final String SEQ_3_S = "23456789";

    public enum MODE {
        ALL,
        ONLY_LOWER_LETTER,
        ONLY_UPPER_LETTER,
        ONLY_LETTER,
        ONLY_NUMBER
    }

    public static String genNChars(int n) {
        return genNChars(n, MODE.ALL, false);
    }

    public static String genNChars(int n, MODE mode) {
        return genNChars(n, mode, false);
    }

    public static String genNChars(int n, MODE mode, Boolean noConfusion) {
        String str = "";
        switch (mode) {
            case ALL:
                if (noConfusion) {
                    str = SEQ_1_S + SEQ_2_S + SEQ_3_S;
                } else {
                    str = SEQ_1 + SEQ_2 + SEQ_3;
                }
                break;
            case ONLY_LETTER:
                if (noConfusion) {
                    str = SEQ_1_S + SEQ_2_S;
                } else {
                    str = SEQ_1 + SEQ_2;
                }
                break;
            case ONLY_NUMBER:
                str = noConfusion ? SEQ_3_S : SEQ_3;
                break;
            case ONLY_LOWER_LETTER:
                str = noConfusion ? SEQ_1_S : SEQ_1;
                break;
            case ONLY_UPPER_LETTER:
                str = noConfusion ? SEQ_2_S : SEQ_2;
                break;
        }

        int max = str.length();
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<n;i++) {
            int idx = random.nextInt(max);
            sb.append(str.charAt(idx));
        }
        return sb.toString();
    }

    public static void main(String... args) {
        for (int i = 0; i < 10; i++) {
            String s = genNChars(8);
            System.out.println(s);
        }
    }
}
