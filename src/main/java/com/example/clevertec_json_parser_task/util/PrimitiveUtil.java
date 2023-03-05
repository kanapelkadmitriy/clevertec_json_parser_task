package com.example.clevertec_json_parser_task.util;


import com.example.clevertec_json_parser_task.exception.BusinessException;

public class PrimitiveUtil {

    public static Object getPrimitiveValue(String primitiveName, String value) {
        switch (primitiveName) {
            case ("int"):
                return Integer.parseInt(value);
            case ("short"):
                return Short.parseShort(value);
            case ("byte"):
                return Byte.parseByte(value);
            case ("long"):
                return Long.parseLong(value);
            case ("float"):
                return Float.parseFloat(value);
            case ("double"):
                return Double.parseDouble(value);
            case ("boolean"):
                return Boolean.parseBoolean(value);
            case ("char"):
                if (value.length() > 1) {
                    throw new BusinessException("invalid value for char");
                }
                return value.charAt(0);
        }
        return null;
    }
}
