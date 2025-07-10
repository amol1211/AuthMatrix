package amol.com.AuthMatrix.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import amol.com.AuthMatrix.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

}
