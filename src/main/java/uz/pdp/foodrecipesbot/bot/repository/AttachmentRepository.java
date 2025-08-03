package uz.pdp.foodrecipesbot.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.foodrecipesbot.bot.models.entity.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}