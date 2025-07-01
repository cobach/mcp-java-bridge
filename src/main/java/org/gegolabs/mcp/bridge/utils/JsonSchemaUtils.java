package org.gegolabs.mcp.bridge.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Utility class for generating JSON schemas from Java classes.
 * Migrated and adapted from uMCP's MiscTools.
 */
@Slf4j
public class JsonSchemaUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Gson gson = new Gson();
    
    /**
     * Generates a JSON schema for a given class.
     * 
     * @param clazz The class to generate schema for
     * @return JSON schema as string
     */
    public static String generateJsonSchema(Class<?> clazz) {
        JsonObject schema = generateSchemaObject(clazz);
        return gson.toJson(schema);
    }
    
    /**
     * Generates a JSON schema object for a given class.
     * 
     * @param clazz The class to generate schema for
     * @return JSON schema as JsonObject
     */
    public static JsonObject generateSchemaObject(Class<?> clazz) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            JsonObject fieldSchema = getFieldSchema(field);
            if (fieldSchema != null) {
                properties.add(toCamelCase(fieldName), fieldSchema);
            }
        }
        
        schema.add("properties", properties);
        return schema;
    }
    
    private static JsonObject getFieldSchema(Field field) {
        JsonObject fieldSchema = new JsonObject();
        Class<?> fieldType = field.getType();
        
        if (fieldType == String.class) {
            fieldSchema.addProperty("type", "string");
        } else if (fieldType == int.class || fieldType == Integer.class) {
            fieldSchema.addProperty("type", "integer");
        } else if (fieldType == long.class || fieldType == Long.class) {
            fieldSchema.addProperty("type", "integer");
        } else if (fieldType == double.class || fieldType == Double.class) {
            fieldSchema.addProperty("type", "number");
        } else if (fieldType == float.class || fieldType == Float.class) {
            fieldSchema.addProperty("type", "number");
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            fieldSchema.addProperty("type", "boolean");
        } else if (List.class.isAssignableFrom(fieldType)) {
            fieldSchema.addProperty("type", "array");
            
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    JsonObject itemSchema = new JsonObject();
                    Class<?> itemClass = (Class<?>) typeArgs[0];
                    itemSchema.addProperty("type", getSimpleTypeName(itemClass));
                    fieldSchema.add("items", itemSchema);
                }
            }
        } else if (Map.class.isAssignableFrom(fieldType)) {
            fieldSchema.addProperty("type", "object");
        } else {
            // Complex object
            fieldSchema.addProperty("type", "object");
            // Could recursively generate schema for nested objects
        }
        
        // Add description if available (could be from annotations)
        Description desc = field.getAnnotation(Description.class);
        if (desc != null) {
            fieldSchema.addProperty("description", desc.value());
        }
        
        return fieldSchema;
    }
    
    private static String getSimpleTypeName(Class<?> clazz) {
        if (clazz == String.class) return "string";
        if (clazz == Integer.class || clazz == int.class) return "integer";
        if (clazz == Long.class || clazz == long.class) return "integer";
        if (clazz == Double.class || clazz == double.class) return "number";
        if (clazz == Float.class || clazz == float.class) return "number";
        if (clazz == Boolean.class || clazz == boolean.class) return "boolean";
        return "object";
    }
    
    /**
     * Converts a string to camelCase.
     * 
     * @param str The string to convert
     * @return The camelCase version
     */
    public static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        // Handle snake_case to camelCase
        if (str.contains("_")) {
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = false;
            
            for (char c : str.toCharArray()) {
                if (c == '_') {
                    capitalizeNext = true;
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
            
            return result.toString();
        }
        
        // Already in camelCase or single word
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
    
    /**
     * Annotation for field descriptions in JSON schema.
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
    public @interface Description {
        String value();
    }
}