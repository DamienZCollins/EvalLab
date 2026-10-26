package com.evallab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.evallab.infra.db.mapper")
public class EvalLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvalLabApplication.class, args);
    }
}
