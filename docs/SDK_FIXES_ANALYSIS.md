# Análisis de Fixes del SDK 0.10.1

## Resumen de Fixes

El SDK 0.10.1-SNAPSHOT contiene dos fixes críticos que permiten el funcionamiento correcto del bridge:

### 1. Fix de Timeout en McpServerSession (commit 6a683b5)

**Problema**: El servidor no enviaba respuestas debido a un orden incorrecto de operadores en la cadena reactiva.

**Síntoma**: Los clientes experimentaban timeouts esperando respuestas del servidor.

**Causa**: El operador `flatMap` estaba colocado después de `onErrorResume`, causando que la lógica de envío de respuestas se saltara cuando ocurrían errores.

**Solución implementada**:
```java
// ANTES (incorrecto):
return handleIncomingRequest(request)
    .onErrorResume(error -> {
        // Error handling
        return this.transport.sendMessage(errorResponse).then(Mono.empty());
    })
    .flatMap(this.transport::sendMessage);

// DESPUÉS (correcto):
return handleIncomingRequest(request)
    .flatMap(response -> this.transport.sendMessage(response))
    .onErrorResume(error -> {
        // Error handling
        return this.transport.sendMessage(errorResponse);
    });
```

**Archivo afectado**: `io.modelcontextprotocol.spec.McpServerSession`

### 2. Fix de Deserialización CallToolRequest (commit cd11d20)

**Problema**: ClassCastException al intentar procesar llamadas a herramientas.

**Síntoma**: Las herramientas no podían ser invocadas debido a un error de casting.

**Causa**: JSON-RPC deserializa los params como `LinkedHashMap`, no como `CallToolRequest`.

**Solución implementada**:
```java
// ANTES (incorrecto):
McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(params,
    new TypeReference<McpSchema.CallToolRequest>() {});

// DESPUÉS (correcto):
McpSchema.CallToolRequest callToolRequest;
if (params instanceof McpSchema.CallToolRequest) {
    callToolRequest = (McpSchema.CallToolRequest) params;
} else if (params instanceof java.util.Map) {
    callToolRequest = objectMapper.convertValue(params, McpSchema.CallToolRequest.class);
} else {
    // Intento de conversión genérica
}
```

**Archivo afectado**: `io.modelcontextprotocol.server.McpAsyncServer`

## Clasificación de Fixes

### Por Criticidad:
- **CRÍTICO**: Fix de timeout - Sin este fix, ninguna respuesta llega al cliente
- **CRÍTICO**: Fix de CallToolRequest - Sin este fix, las herramientas no funcionan

### Por Tipo:
- **Bug Fix**: Ambos son correcciones de bugs, no mejoras o nuevas features
- **Comportamiento interno**: No cambian la API pública, solo la implementación

### Por Complejidad:
- **Simple**: Fix de timeout - Solo reordenar operadores
- **Moderada**: Fix de CallToolRequest - Requiere type checking y conversión

## Viabilidad de Aplicación en Runtime

### Fix 1 - Timeout (McpServerSession)

**Técnicas aplicables**:
1. **Wrapper/Proxy**: ✅ Crear un wrapper de McpServerSession
2. **ByteBuddy**: ✅ Reescribir el método en tiempo de carga
3. **AspectJ**: ✅ Interceptar el método handleMessage
4. **Reflection**: ❌ Difícil porque involucra cadenas reactivas

**Recomendación**: Wrapper/Proxy por simplicidad

### Fix 2 - CallToolRequest (McpAsyncServer)

**Técnicas aplicables**:
1. **Wrapper/Proxy**: ✅ Interceptar toolsCallRequestHandler
2. **ByteBuddy**: ✅ Modificar el método directamente
3. **AspectJ**: ✅ Around advice en el handler
4. **Reflection**: ⚠️ Posible pero complejo

**Recomendación**: Wrapper/Proxy para consistencia

## Estrategia Recomendada

Basándome en el análisis, recomiendo:

1. **Técnica principal**: Wrapper/Proxy Pattern
   - Más simple y mantenible
   - No requiere dependencias adicionales
   - Fácil de testear

2. **Implementación**:
   - Crear wrappers para las clases afectadas
   - Interceptar los métodos problemáticos
   - Aplicar las correcciones antes de delegar

3. **Activación**:
   - Automática al crear el bridge
   - Transparente para el usuario
   - Con logging opcional

## Próximos Pasos

1. Implementar PoC del wrapper para McpServerSession
2. Validar que el fix de timeout funciona
3. Implementar wrapper para McpAsyncServer
4. Crear suite de tests
5. Documentar limitaciones

🤖 Generated with Claude Wing Coding support (https://claude.ai)