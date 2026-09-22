package studdybuddy.message;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import studdybuddy.user.User;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected Message() {
    }

    public Message(
            User sender,
            User receiver,
            String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    @PrePersist
    void setSentAt() {sentAt = Instant.now();}

    public Long getId() {return id;}

    public User getSender() {return sender;}

    public User getReceiver() {return receiver;}

    public String getContent() {return content;}

    public Instant getSentAt() {return sentAt;}
}
