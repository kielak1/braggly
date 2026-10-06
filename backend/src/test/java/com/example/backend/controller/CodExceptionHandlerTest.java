package com.example.backend.controller;
import com.example.backend.service.*;
import com.example.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CodExceptionHandlerTest {
    @Test void queueRejectionIsAnExplicit429WithFailedStatus() throws Exception {
        var service=mock(CodImportService.class);
        when(service.checkAndImport(anyList(),anyBoolean())).thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"COD import queue is full"));
        var mvc=MockMvcBuilders.standaloneSetup(new CodController(service,mock(CodFormulaLookupService.class),mock(CodQueryRepository.class)))
                .setControllerAdvice(new CodExceptionHandler()).build();
        mvc.perform(post("/api/cod/search").contentType("text/plain").content("H O"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.status").value("FAILED"));
    }
    @Test void oversizedCifReturns413WithoutServletErrorForwarding() throws Exception {
        var service=mock(CifInfoService.class);
        when(service.getStructureInfo(anyString())).thenThrow(new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"CIF size limit exceeded"));
        var mvc=MockMvcBuilders.standaloneSetup(new CodCifController(service)).setControllerAdvice(new CodExceptionHandler()).build();
        mvc.perform(get("/api/cod/cif/123")).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.status").value("FAILED"));
    }
}
