package amol.com.authmatrixbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import amol.com.authmatrixbackend.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);
    
    Boolean existsByEmail(String email);

   
}
