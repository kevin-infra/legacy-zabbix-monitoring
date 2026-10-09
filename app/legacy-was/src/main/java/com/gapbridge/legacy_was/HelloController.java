package com.gapbridge.legacy_was;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }

    @GetMapping("/load")
    public String load() {
        long start = System.currentTimeMillis();
        long count = 0;
        for (long i = 0; i < 500_000_000L; i++) {
            count += i % 7;
        }
        long elapsedMs = System.currentTimeMillis() - start;
        return "load test done. count=" + count + ", elapsedMs=" + elapsedMs;
    }
}
