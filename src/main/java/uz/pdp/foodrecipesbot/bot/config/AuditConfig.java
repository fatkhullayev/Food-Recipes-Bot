package uz.pdp.foodrecipesbot.bot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            try {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                    return Optional.of(auth.getName());
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            return Optional.of("BOT_SYSTEM");
        };
    }

   /* @Bean
    public AuditorAware<String> auditorProvider() {
        // Bu yerda tizim nomidan foydalanuvchini yozamiz
        return () -> Optional.of("BOT_SYSTEM"); // yoki agar login bo‘lsa: SecurityContext'dan oling
    }*/
}
