package org.gegolabs.mcp.bridge.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaUtilsTest {
    
    static class SimpleClass {
        private String name;
        private int age;
        private boolean active;
    }
    
    static class AnnotatedClass {
        @JsonSchemaUtils.Description("User's full name")
        private String name;
        
        @JsonSchemaUtils.Description("User's age in years")
        private Integer age;
        
        @JsonSchemaUtils.Description("Whether the user is active")
        private Boolean active;
    }
    
    static class ComplexClass {
        @JsonSchemaUtils.Description("User information")
        private AnnotatedClass user;
        
        @JsonSchemaUtils.Description("List of tags")
        private List<String> tags;
        
        @JsonSchemaUtils.Description("Additional properties")
        private JsonObject metadata;
    }
    
    @Test
    void testGenerateSimpleSchema() {
        String schema = JsonSchemaUtils.generateJsonSchema(SimpleClass.class);
        assertNotNull(schema);
        
        JsonObject json = JsonParser.parseString(schema).getAsJsonObject();
        assertEquals("object", json.get("type").getAsString());
        
        JsonObject properties = json.getAsJsonObject("properties");
        assertTrue(properties.has("name"));
        assertTrue(properties.has("age"));
        assertTrue(properties.has("active"));
        
        assertEquals("string", properties.getAsJsonObject("name").get("type").getAsString());
        assertEquals("integer", properties.getAsJsonObject("age").get("type").getAsString());
        assertEquals("boolean", properties.getAsJsonObject("active").get("type").getAsString());
    }
    
    @Test
    void testGenerateAnnotatedSchema() {
        String schema = JsonSchemaUtils.generateJsonSchema(AnnotatedClass.class);
        assertNotNull(schema);
        
        JsonObject json = JsonParser.parseString(schema).getAsJsonObject();
        JsonObject properties = json.getAsJsonObject("properties");
        
        JsonObject nameProperty = properties.getAsJsonObject("name");
        assertEquals("User's full name", nameProperty.get("description").getAsString());
        
        JsonObject ageProperty = properties.getAsJsonObject("age");
        assertEquals("User's age in years", ageProperty.get("description").getAsString());
        
        JsonObject activeProperty = properties.getAsJsonObject("active");
        assertEquals("Whether the user is active", activeProperty.get("description").getAsString());
    }
    
    @Test
    void testGenerateComplexSchema() {
        String schema = JsonSchemaUtils.generateJsonSchema(ComplexClass.class);
        assertNotNull(schema);
        
        JsonObject json = JsonParser.parseString(schema).getAsJsonObject();
        JsonObject properties = json.getAsJsonObject("properties");
        
        // Check nested object
        JsonObject userProperty = properties.getAsJsonObject("user");
        assertEquals("object", userProperty.get("type").getAsString());
        assertEquals("User information", userProperty.get("description").getAsString());
        
        // Check array
        JsonObject tagsProperty = properties.getAsJsonObject("tags");
        assertEquals("array", tagsProperty.get("type").getAsString());
        assertEquals("List of tags", tagsProperty.get("description").getAsString());
        
        // Check object (JsonObject)
        JsonObject metadataProperty = properties.getAsJsonObject("metadata");
        assertEquals("object", metadataProperty.get("type").getAsString());
        assertEquals("Additional properties", metadataProperty.get("description").getAsString());
    }
}