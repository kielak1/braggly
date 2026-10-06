package com.example.backend.service;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
class LimitedInputStreamTest {
    @Test void allowsExactlyTheConfiguredLimitAndRejectsTheNextByte() throws Exception {
        try(var input=new LimitedInputStream(new ByteArrayInputStream(new byte[10]),10)) {assertThat(input.readAllBytes()).hasSize(10);}
        try(var input=new LimitedInputStream(new ByteArrayInputStream(new byte[11]),10)) {
            assertThat(input.readNBytes(10)).hasSize(10);assertThatThrownBy(input::read).isInstanceOf(LimitedInputStream.SizeLimitExceededException.class);
        }
    }
    @Test void skipCannotBypassTheByteLimit() throws Exception {
        try(var input=new LimitedInputStream(new ByteArrayInputStream(new byte[11]),10)) {
            assertThatThrownBy(() -> input.skip(11)).isInstanceOf(LimitedInputStream.SizeLimitExceededException.class);
        }
    }
}
