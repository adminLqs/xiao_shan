package com.xiaoshan.springbootdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 启用定时任务
public class SpringBootDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemoApplication.class, args);
    }

}
/*
src/main/java/
        └── com/xiaoshan/springbootdemo/
        ├── DemoApplication.java        # 主应用类
    ├── controller/                # 控制器层
    ├── service/                   # 服务层
    ├── repository/                # 数据访问层
    ├── entity/                    # 实体类
    └── config/                    # 配置类
src/main/resources/
        ├── application-dev.yml         # 配置文件
├── static/                        # 静态资源
├── templates/                     # 模板文件
└── resources/
src/test/java/                     # 测试代码
mvnw/mvnw.cmd                      # Maven wrapper
pom.xml                            # 项目依赖配置
 */