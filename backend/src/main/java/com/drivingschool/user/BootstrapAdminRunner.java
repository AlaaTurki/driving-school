package com.drivingschool.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private final boolean enabled;
    private final String email;
    private final String password;
    private final BootstrapAdminService bootstrapAdminService;

    public BootstrapAdminRunner(
            @Value("${app.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            BootstrapAdminService bootstrapAdminService
    ) {
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.bootstrapAdminService = bootstrapAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (enabled) {
            bootstrapAdminService.createFirstAdmin(email, password);
        }
    }
}
