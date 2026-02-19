package com.elias.attendancecontrol;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class AttendaceControlApplication {
    public static void main(String[] args) {
        SpringApplication.run(AttendaceControlApplication.class, args);
    }
}
