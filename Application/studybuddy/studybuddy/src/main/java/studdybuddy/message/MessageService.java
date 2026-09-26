package studdybuddy.message;

import java.util.List;
import org.springframework.stereotype.Service;
import studdybuddy.user.User;
import studdybuddy.user.UserRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }


    public MessageResponse sendMessage(
            String currentEmail,
            Long receiverId,
            String content) {
        User sender = userRepository
                .findByEmail(currentEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Current user not found"));
        User receiver = userRepository
                .findById(receiverId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Receiver not found"));
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException(
                    "You cannot message yourself");
        }
        Message message = new Message(sender, receiver, content.trim());
        Message saved = messageRepository.save(message);
        return toResponse(saved);
    }

    public List<MessageResponse> getConversation(
            String currentEmail,
            Long otherUserId) {
        User currentUser = userRepository
                .findByEmail(currentEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Current user not found"));

        User otherUser = userRepository
                .findById(otherUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"));

        return messageRepository
                .findConversation(currentUser, otherUser)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private MessageResponse toResponse(
            Message message) {
        String senderName = message.getSender().getFirstName() + " " + message.getSender().getLastName();
        String receiverName = message.getReceiver().getFirstName() + " " + message.getReceiver().getLastName();

        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                senderName,
                message.getReceiver().getId(),
                receiverName,
                message.getContent(),
                message.getSentAt()
        );
    }
}
