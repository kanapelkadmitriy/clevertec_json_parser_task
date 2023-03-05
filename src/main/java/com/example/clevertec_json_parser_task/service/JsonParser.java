package com.example.clevertec_json_parser_task.service;

public interface JsonParser<T> {

    T parseToObject(String json, Class<?> clazz);

    String parseToJson(T t);
}
