package sync2.chatApp.repository;

import org.springframework.stereotype.Repository;
import sync2.chatApp.entity.User;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findById(UUID id);
	Optional<User> findByUsername(String username);
	boolean existsByUsername(String username);
	boolean existsByEmail(String email);
}
