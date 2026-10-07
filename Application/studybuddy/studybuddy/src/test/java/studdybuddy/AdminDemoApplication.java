package studdybuddy;

import org.springframework.boot.SpringApplication;

/** Local demo only, using a disposable database and a loopback listener. */
public final class AdminDemoApplication {
    public static void main(String[] args) {
        SpringApplication.from(StudybuddyApplication::main)
                .with(AdminDemoConfiguration.class)
                .run("--spring.datasource.url=jdbc:h2:mem:admin_demo",
                     "--spring.datasource.driver-class-name=org.h2.Driver",
                     "--spring.datasource.username=sa", "--spring.datasource.password=",
                     "--spring.flyway.enabled=false", "--spring.jpa.hibernate.ddl-auto=create-drop",
                     "--server.address=127.0.0.1", "--server.port=18081");
    }
}
