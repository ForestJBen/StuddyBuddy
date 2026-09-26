package studdybuddy.message;

import java.time.Instant;

public class MessageResponse {

    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private Instant sentAt;

    public MessageResponse(
            Long id,
            Long senderId,
            String senderName,
            Long receiverId,
            String receiverName,
            String content,
            Instant sentAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.content = content;
        this.sentAt = sentAt;
    }

    public Long getId() {return id;}

    public Long getSenderId() {return senderId;}

    public String getSenderName() {return senderName;}

    public Long getReceiverId() {return receiverId;}

    public String getReceiverName() {return receiverName;}

    public String getContent() {return content;}

    public Instant getSentAt() {return sentAt;}
}
