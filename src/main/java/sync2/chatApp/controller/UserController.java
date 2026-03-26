package sync2.chatApp.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import sync2.chatApp.model.LoginUserRequest;
import sync2.chatApp.model.RegisterUserRequest;
import sync2.chatApp.model.UpdateUserRequest;
import sync2.chatApp.model.WebResponse;
import sync2.chatApp.service.UserService;

@RestController
@AllArgsConstructor
public class UserController {
	private UserService userService;
	
	@PostMapping(path = "/auth/register")
	public WebResponse<String> register(@RequestBody RegisterUserRequest request){
		userService.register(request);
		return WebResponse.<String>builder().data("User registered successfully").build();
	}
	
	@PostMapping(path = "/auth/login")
	private WebResponse<String> login(@RequestBody LoginUserRequest request){
		String token = userService.login(request);
		return WebResponse.<String>builder().data(token).build();
	}
	
	@PatchMapping(path = "/user/update")
	private WebResponse<String> update(@RequestBody UpdateUserRequest request){
		userService.update(request);
		return WebResponse.<String>builder().data("User updated successfully").build();
	}
}
