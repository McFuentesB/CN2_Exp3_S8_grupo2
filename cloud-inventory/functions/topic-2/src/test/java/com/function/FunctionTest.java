package com.function;

import com.function.warehouse.WarehouseFunction;
import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test básico de GET /api/warehouses
 */
public class FunctionTest {

    @Test
    public void testGetWarehouses_ok() {
        // Mocks
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        when(req.getQueryParameters()).thenReturn(Collections.emptyMap());
        when(req.getBody()).thenReturn(Optional.empty());

        when(req.createResponseBuilder(any(HttpStatus.class)))
                .thenAnswer(inv -> {
                    HttpStatus status = (HttpStatus) inv.getArguments()[0];
                    return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
                });

        final ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getGlobal());

        // Invoke
        HttpResponseMessage res = new WarehouseFunction().getWarehouses(req, context);

        // Assert
        assertEquals(HttpStatus.OK, res.getStatus());
    }
}
