package com.wawa.utils;

/**
 * @author kevin
 * @date 2020/06/16
 */
public class UnicodeUtil {


    /**
     * 把中文转成Unicode码
     *
     * @param str
     * @return
     */
    public static String chinese2Unicode(String str) {
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            int chr1 = (char) str.charAt(i);
            if (chr1 >= 19968 && chr1 <= 171941) {// 汉字范围 \u4e00-\u9fa5 (中文)
                result += "\\u" + Integer.toHexString(chr1);
            } else {
                result += str.charAt(i);
            }
        }
        return result;
    }

    /**
     * 判断是否为中文字符
     *
     * @param c
     * @return
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }


    //Unicode转中文
    public static String unicode2Chinese(final String unicode) {
        StringBuffer string = new StringBuffer();

        String[] hex = unicode.split("\\\\u");

        for (int i = 0; i < hex.length; i++) {

            try {
                // 汉字范围 \u4e00-\u9fa5 (中文)
                if(hex[i].length()>=4){//取前四个，判断是否是汉字
                    String chinese = hex[i].substring(0, 4);
                    try {
                        int chr = Integer.parseInt(chinese, 16);
                        boolean isChinese = isChinese((char) chr);
                        //转化成功，判断是否在  汉字范围内
                        if (isChinese){//在汉字范围内
                            // 追加成string
                            string.append((char) chr);
                            //并且追加  后面的字符
                            String behindString = hex[i].substring(4);
                            string.append(behindString);
                        }else {
                            string.append(hex[i]);
                        }
                    } catch (NumberFormatException e1) {
                        string.append(hex[i]);
                    }

                }else{
                    string.append(hex[i]);
                }
            } catch (NumberFormatException e) {
                string.append(hex[i]);
            }
        }

        return string.toString();
    }

    /**
     * unicode解码
     * @param unicode
     * @return
     */
    public static String decodeUnicode(final String unicode) {
        StringBuffer sb = new StringBuffer();

        String[] hex = unicode.split("\\\\u");
        for (int i = 0; i < hex.length; i++) {
            try {
                if(hex[i].length()>=4){
                    //取前四个，判断是否是汉字
                    String chinese = hex[i].substring(0, 4);
                    try {
                        int chr = Integer.parseInt(chinese, 16);
                        // 追加成string
                        sb.append((char) chr);
                        //并且追加  后面的字符
                        String behindString = hex[i].substring(4);
                        sb.append(behindString);

                    } catch (NumberFormatException e1) {
                        sb.append(hex[i]);
                    }

                }else{
                    sb.append(hex[i]);
                }
            } catch (NumberFormatException e) {
                sb.append(hex[i]);
            }
        }

        return sb.toString();
    }


    public static void main(String[] args) {
//        String str = "中文Abc123";
//        String encodingStr = chinese2Unicode(str);
//        System.out.println(encodingStr);
//        String decodeStr = decodeUnicode(encodingStr);
//        System.out.println(decodeStr);

        String str1 = "\\u4e2d\\u6587Abc7777 。\\u003134张文\\u0032\\u0033";
        System.out.println(decodeUnicode(str1));



    }
}
