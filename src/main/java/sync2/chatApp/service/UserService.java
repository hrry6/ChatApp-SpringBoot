package sync2.chatApp.service;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import sync2.chatApp.entity.User;
import sync2.chatApp.model.LoginUserRequest;
import sync2.chatApp.model.RegisterUserRequest;
import sync2.chatApp.model.UpdateUserRequest;
import sync2.chatApp.repository.UserRepository;
import sync2.chatApp.security.JwtUtil;

@Service
@AllArgsConstructor
public class UserService {
	private UserRepository userRepository;
	private Validator validator;
	private BCryptPasswordEncoder passwordEncoder;
	private JwtUtil jwtUtil;

	public void register(RegisterUserRequest request) {
		Set<ConstraintViolation<RegisterUserRequest>> constraintViolations = validator.validate(request);
		if (!constraintViolations.isEmpty())
			throw new ConstraintViolationException(constraintViolations);

		if (userRepository.existsByUsername(request.getUsername()))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already registered database");

		if (userRepository.existsByEmail(request.getEmail()))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered in database");

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());

		String hashedPassword = passwordEncoder.encode(request.getPassword());
		user.setPasswordHash(hashedPassword);

		userRepository.save(user);
	}

	public String login(LoginUserRequest request) {
		Set<ConstraintViolation<LoginUserRequest>> constraintViolations = validator.validate(request);
		if (!constraintViolations.isEmpty())
			throw new ConstraintViolationException(constraintViolations);

		User user = userRepository.findByUsername(request.getUsername()).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");

		return jwtUtil.generateToken(user);
	}

	@Transactional
	public void update(UpdateUserRequest request) {
		Set<ConstraintViolation<UpdateUserRequest>> constraintViolations = validator.validate(request);
		if (!constraintViolations.isEmpty())
			throw new ConstraintViolationException(constraintViolations);

		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
	        if (request.getPrevPassword() == null || request.getPrevPassword().isBlank()) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
	                "Previous password is required to change password");
	        }

	        if (!passwordEncoder.matches(request.getPrevPassword(), user.getPasswordHash())) {
	            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Previous password does not match");
	        }

	        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
	    }

	    if (request.getUsername() != null && !request.getUsername().isBlank()) {
	        if (!request.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
	        }
	        user.setUsername(request.getUsername());
	    }

	    if (request.getEmail() != null && !request.getEmail().isBlank()) {
	        user.setEmail(request.getEmail());
	    }
	    
	    userRepository.save(user);
	}
}
