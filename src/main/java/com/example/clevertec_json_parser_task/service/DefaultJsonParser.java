package com.example.clevertec_json_parser_task.service;


import com.example.clevertec_json_parser_task.exception.BusinessException;
import com.example.clevertec_json_parser_task.util.JsonProcessorUtil;
import com.example.clevertec_json_parser_task.util.PrimitiveUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.clevertec_json_parser_task.util.Constants.BASIC_PATTERN;
import static com.example.clevertec_json_parser_task.util.Constants.CLOSE_ARRAY_BRACKET;
import static com.example.clevertec_json_parser_task.util.Constants.CLOSE_FIGURE_BRACKET;
import static com.example.clevertec_json_parser_task.util.Constants.COMMA;
import static com.example.clevertec_json_parser_task.util.Constants.OPEN_ARRAY_BRACKET;
import static com.example.clevertec_json_parser_task.util.Constants.OPEN_FIGURE_BRACKET;
import static com.example.clevertec_json_parser_task.util.Constants.TO_STRING_PATTERN;


public class DefaultJsonParser<T> implements JsonParser<T> {

    @Override
    public T parseToObject(String json, Class<?> clazz) {
        final ReflectionService<T> reflectionService = new ReflectionService<>();

        final Map<String, String> fieldValueData = JsonProcessorUtil.createFieldValueData(json);
        T newInstance = reflectionService.createEmptyInstance(clazz);
        try {
            Field[] fields = newInstance.getClass().getDeclaredFields();
            for (Field field : fields) {
                String value = fieldValueData.get(field.getName());
                field.setAccessible(true);
                if (field.getType().isPrimitive()) {
                    String primitiveName = field.getType().getName();
                    field.set(newInstance, PrimitiveUtil.getPrimitiveValue(primitiveName, value));
                    continue;
                }

                Optional<Method> valueOfMethod = reflectionService.getValueOfMethod(field.getType());
                if (value.startsWith(OPEN_FIGURE_BRACKET.toString()) && value.endsWith(CLOSE_FIGURE_BRACKET.toString())) {
                    Object result = new DefaultJsonParser<>().parseToObject(value, field.getType());
                    field.set(newInstance, field.getType().cast(result));
                    continue;
                }
                if (value.startsWith(OPEN_ARRAY_BRACKET.toString()) && value.endsWith(CLOSE_ARRAY_BRACKET.toString())) {
                    arrayProcessing(reflectionService, newInstance, value, field);
                    continue;
                }
                if (valueOfMethod.isPresent()) {
                    field.set(newInstance, valueOfMethod.get().invoke(null, value));
                    continue;
                }
                field.set(newInstance, value);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BusinessException("exception occurred during setting values to fields");
        }
        return newInstance;
    }

    @Override
    public String parseToJson(T t) {
        final StringBuilder jsonSb = new StringBuilder().append(OPEN_FIGURE_BRACKET);

        Field[] declaredFields = t.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object value = field.get(t);

                final String jsonValue = isToStringObject(value)
                        ? getValue(fieldName, value, TO_STRING_PATTERN)
                        : isCustomObject(field, value)
                        ? getValue(fieldName, new DefaultJsonParser<>().parseToJson(value), BASIC_PATTERN)
                        : getValue(fieldName, value, BASIC_PATTERN);

                jsonSb.append(jsonValue);

            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessException("exception occurred during getting values to fields");
            }
        }
        return jsonSb.deleteCharAt(jsonSb.length() - 1).append(CLOSE_FIGURE_BRACKET).toString();
    }

    private void arrayProcessing(ReflectionService<T> reflectionService,
                                 T newInstance,
                                 String value,
                                 Field field) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        String[] arrayValues = value.substring(1, value.length() - 1).split(COMMA.toString());

        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Type genericType = parameterizedType.getActualTypeArguments()[0];

        List<Object> valueList = new ArrayList<>();
        final Optional<Method> valueOfGenericMethod = reflectionService
                .getValueOfMethod(Class.forName(((Class<?>) genericType).getName()));
        if (valueOfGenericMethod.isPresent()) {
            for (String arrayValue : arrayValues) {
                valueList.add(valueOfGenericMethod.get().invoke(null, arrayValue));
            }
        } else {
            valueList.addAll(Arrays.asList(arrayValues));
        }
        field.set(newInstance, field.getType().cast(valueList));
    }

    private boolean isCustomObject(Field field, Object value) {
        return !field.getType().isPrimitive()
                && !(value instanceof Boolean)
                && !(value instanceof Number)
                && !(value instanceof Collection<?>);
    }

    private boolean isToStringObject(Object value) {
        return value instanceof String
                || value instanceof Temporal
                || value instanceof Character;
    }

    private String getValue(String fieldName, Object value, String pattern) {
        return String.format(pattern, fieldName, value);
    }
}
