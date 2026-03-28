package sync2.chatApp.service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import sync2.chatApp.entity.Chat;
import sync2.chatApp.entity.Message;
import sync2.chatApp.entity.User;
import sync2.chatApp.model.MessageResponse;
import sync2.chatApp.model.SendMessageRequest;
import sync2.chatApp.repository.ChatMemberRepository;
import sync2.chatApp.repository.ChatRepository;
import sync2.chatApp.repository.MessageRepository;

@Service
@AllArgsConstructor
public class MessageService {
	private final ChatRepository chatRepository;
	private final ChatMemberRepository chatMemberRepository;
	private final MessageRepository messageRepository;
	private final Validator validator;
	
	@Transactional
	public List<MessageResponse> getChatMessages(UUID chatId) {
		User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Chat chat = chatRepository.findById(chatId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));

		if (!chatMemberRepository.existsByChatIdAndUserId(chatId, currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to veiew messages");
		}

		List<Message> messages = messageRepository.findAllByChatIdOrderByCreatedAtAsc(chatId);

		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

		return messages.stream().map(msg -> {
			MessageResponse.MessageResponseBuilder builder = MessageResponse.builder().id(msg.getId())
					.content(msg.getContent()).iv(msg.getIv()).type(msg.getType()).sender(msg.getSender().getUsername())
					.time(msg.getCreatedAt().format(timeFormatter));

			if ("GROUP".equals(chat.getType())) {
				builder.sender(msg.getSender().getUsername());
			}

			return builder.build();
		}).collect(Collectors.toList());
	}

	@Transactional
	public MessageResponse sendMessage(UUID chatId, SendMessageRequest request, Principal principal){
		Set<ConstraintViolation<SendMessageRequest>> constraintViolations = validator.validate(request);
		if (!constraintViolations.isEmpty())
			throw new ConstraintViolationException(constraintViolations);

		Authentication authentication = (Authentication) principal;
    	User currentUser = (User) authentication.getPrincipal();

		Chat chat = chatRepository.findById(chatId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));

		if (!chatMemberRepository.existsByChatIdAndUserId(chatId, currentUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not member of this chat");
		}

		Message message = new Message();
		message.setId(UUID.randomUUID());
		message.setChat(chat);
		message.setSender(currentUser);
		message.setContent(request.getContent());
		message.setIv(request.getIv());
		message.setType(request.getType());
		message.setCreatedAt(LocalDateTime.now());

		messageRepository.save(message);

		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

		return MessageResponse.builder().id(message.getId()).content(message.getContent()).iv(message.getIv())
				.type(message.getType()).time(message.getCreatedAt().format(timeFormatter))
				.sender(currentUser.getUsername()).build();
	}

}
