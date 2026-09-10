package com.itsheep.commentdetails.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
/**
 * @BelongsProject: plugin-comment-details
 * @BelongsPackage: com.itsheep.commentdetails.controller
 * @Author: cheny
 * @CreateTime: 2026-09-10  09:27
 * @Description: TODO
 * @Version: 1.0
 */
@RestController
@RequestMapping("/api/v1alpha1/my-plugin")
public class DemoController {

    @GetMapping("/hello")
    public Mono<String> sayHello() {
        return Mono.just("Hello from Halo Plugin!");
    }
}
