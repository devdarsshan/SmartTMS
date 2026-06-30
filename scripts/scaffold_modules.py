import os

ROOT_DIR = r"f:\Gen_Projects\SmartTMS"

CONFIG_SERVER_POM = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.smartlogistics</groupId>
		<artifactId>smart-tms</artifactId>
		<version>1.0.0</version>
		<relativePath>../pom.xml</relativePath>
	</parent>
	<artifactId>config-server</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>config-server</name>
	<description>Config Server for SmartTMS</description>
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-config-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
"""

CONFIG_SERVER_APP = """package com.smartlogistics.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
"""

CONFIG_SERVER_PROPS = """spring.application.name=config-server
server.port=8888
spring.profiles.active=native
spring.cloud.config.server.native.search-locations=file:///${user.dir}/config-repo
"""

NOTIFICATION_POM = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.smartlogistics</groupId>
		<artifactId>smart-tms</artifactId>
		<version>1.0.0</version>
		<relativePath>../pom.xml</relativePath>
	</parent>
	<artifactId>notification-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>notification-service</name>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
</project>
"""

NOTIFICATION_APP = """package com.smartlogistics.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
"""

NOTIFICATION_LISTENER = """package com.smartlogistics.notification.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {
    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void handleOrderEvent(String event) {
        System.out.println("Received order event: " + event);
        // TODO: Send email/SMS notification based on event state
    }
}
"""

NOTIFICATION_PROPS = """spring.application.name=notification-service
server.port=7088
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-group
spring.datasource.url=jdbc:mysql://localhost:3306/notification_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/
"""

def create_module(name, pom, app_code, props, listener_code=None):
    mod_dir = os.path.join(ROOT_DIR, name)
    os.makedirs(mod_dir, exist_ok=True)
    
    with open(os.path.join(mod_dir, "pom.xml"), "w") as f:
        f.write(pom)
        
    src_main_java = os.path.join(mod_dir, "src", "main", "java", "com", "smartlogistics", name.split('-')[0])
    os.makedirs(src_main_java, exist_ok=True)
    
    if "ConfigServer" in app_code:
        app_file = "ConfigServerApplication.java"
    else:
        app_file = "NotificationServiceApplication.java"
        
    with open(os.path.join(src_main_java, app_file), "w") as f:
        f.write(app_code)
        
    if listener_code:
        listener_dir = os.path.join(src_main_java, "listener")
        os.makedirs(listener_dir, exist_ok=True)
        with open(os.path.join(listener_dir, "OrderEventListener.java"), "w") as f:
            f.write(listener_code)
            
    res_dir = os.path.join(mod_dir, "src", "main", "resources")
    os.makedirs(res_dir, exist_ok=True)
    with open(os.path.join(res_dir, "application.properties"), "w") as f:
        f.write(props)

def update_root_pom():
    root_pom = os.path.join(ROOT_DIR, "pom.xml")
    with open(root_pom, "r") as f:
        content = f.read()
    
    if "<module>config-server</module>" not in content:
        content = content.replace("</modules>", "    <module>config-server</module>\n        <module>notification-service</module>\n    </modules>")
        with open(root_pom, "w") as f:
            f.write(content)

if __name__ == "__main__":
    create_module("config-server", CONFIG_SERVER_POM, CONFIG_SERVER_APP, CONFIG_SERVER_PROPS)
    create_module("notification-service", NOTIFICATION_POM, NOTIFICATION_APP, NOTIFICATION_PROPS, NOTIFICATION_LISTENER)
    
    os.makedirs(os.path.join(ROOT_DIR, "config-repo"), exist_ok=True)
    update_root_pom()
    print("Scaffolded config-server and notification-service")
