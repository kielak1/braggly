package com.example.backend.service;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
class CifConcurrencyFilterTest {
    MockHttpServletRequest request(){var r=new MockHttpServletRequest();r.setServletPath("/api/cod/cif/123");return r;}
    @Test void retainsTheSlotThroughSerializationAndReleasesItOnFailure() throws Exception {
        var filter=new CifConcurrencyFilter(new CifLimits(4096,100,1,100,100));
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var client=Executors.newSingleThreadExecutor()) {
            var active=client.submit(() -> {filter.doFilter(request(),new MockHttpServletResponse(),(req,res) -> {
                started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}
                throw new java.io.IOException("synthetic serialization failure");
            });return null;});
            try {
                assertThat(started.await(5,TimeUnit.SECONDS)).isTrue();var rejected=new MockHttpServletResponse();
                filter.doFilter(request(),rejected,(req,res) -> {throw new AssertionError("Must not reach controller");});
                assertThat(rejected.getStatus()).isEqualTo(429);
            } finally {release.countDown();}
            assertThatThrownBy(() -> active.get(5,TimeUnit.SECONDS)).hasCauseInstanceOf(java.io.IOException.class);
        }
        var recovered=new MockHttpServletResponse();filter.doFilter(request(),recovered,(req,res)->res.getWriter().write("OK"));
        assertThat(recovered.getContentAsString()).isEqualTo("OK");
    }
}
