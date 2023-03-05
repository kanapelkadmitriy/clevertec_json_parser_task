package com.example.clevertec_json_parser_task.service;


import com.example.clevertec_json_parser_task.exception.BusinessException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static com.example.clevertec_json_parser_task.util.Constants.STRING_CLASS_NAME;
import static com.example.clevertec_json_parser_task.util.Constants.VALUE_OF_METHOD_NAME;


public class ReflectionService<T> {

    public T createEmptyInstance(Class<?> clazz) {
        T newInstance = null;

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Optional<Constructor<?>> emptyConstructorOpt = Arrays.stream(constructors)
                .filter(constructor -> constructor.getParameterTypes().length == 0)
                .findFirst();
        Optional<Constructor<?>> parametrizedConstructorOpt = Arrays.stream(constructors)
                .filter(constructor -> constructor.getParameterTypes().length != 0)
                .findFirst();

        Object[] parameters = null;
        if (parametrizedConstructorOpt.isPresent()) {
            int lengthOfParameters = parametrizedConstructorOpt.get().getParameterTypes().length;
            parameters = new Object[lengthOfParameters];
            for (int i = 0; i < lengthOfParameters; i++) {
                parameters[i] = null;
            }
        }
        try {
            newInstance = emptyConstructorOpt.isPresent()
                    ? (T) emptyConstructorOpt.get().newInstance()
                    : (T) parametrizedConstructorOpt.get().newInstance(parameters);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new BusinessException(String.format(
                    "exception occurred during new %s instance with null fields creation",
                    clazz.getName()));
        }
        return newInstance;
    }

    Optional<Method> getValueOfMethod(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> VALUE_OF_METHOD_NAME.equals(method.getName())
                        && method.getParameterTypes().length == 1
                        && Arrays.stream(method.getParameterTypes())
                        .map(Class::getName)
                        .anyMatch(STRING_CLASS_NAME::equals))
                .findFirst();
    }
}
