package hbnu.project.zhiyanbackend.message.repository;


import hbnu.project.zhiyanbackend.message.model.entity.MessageBody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageBodyRepository extends JpaRepository<MessageBody, Long> {
}
