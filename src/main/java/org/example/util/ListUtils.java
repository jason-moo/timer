package org.example.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

    public static <T> List<List<T>> division(List<T> list, int size) {
        if (list == null && list.size() == 0) {
            return null;
        }
        int total = list.size();
        int num = total / size;
        List<List<T>> lists = new ArrayList<>(num);
        for (int i = 1; i <= num; i++) {
            lists.add(list.subList((i - 1) * size, i * size));
        }
        return lists;
    }


    public static void main(String[] args) {
        List<String> a = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            a.add("a" + i);
        }
        List<List<String>> result = division(a,30);
        System.out.println(result);
    }

}
