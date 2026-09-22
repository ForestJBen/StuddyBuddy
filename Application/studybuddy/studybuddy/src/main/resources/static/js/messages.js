document.addEventListener("DOMContentLoaded", function () {

    loadMessages();

    const form = document.getElementById("message-form");
    form.addEventListener(
        "submit",
        sendMessage
    );
});

function getOtherUserId() {
    const parts = window.location.pathname.split("/");
    return parts[parts.length - 1];
}

async function loadMessages() {
    const userId = getOtherUserId();
    const list = document.getElementById("message-list");

    try {
        const response = await fetch("/api/messages/" + userId);
        if (!response.ok) {
            throw new Error(
                "Could not load messages"
            );
        }
        const messages = await response.json();
        list.innerHTML = "";

        if (messages.length === 0) {
            list.textContent = "No messages yet. Start the conversation.";
            return;
        }

        messages.forEach(function (message) {
            const messageBox = document.createElement("div");
            const sender = document.createElement("strong");
            sender.textContent = message.senderName;
            const content = document.createElement("p");
            content.textContent = message.content;
            messageBox.appendChild(sender);
            messageBox.appendChild(content);
            list.appendChild(messageBox);
        });

    } catch (error) {
        console.log(error);
        list.textContent = "Could not load messages.";
    }
}
async function sendMessage(event) {
    event.preventDefault();
    const userId = getOtherUserId();
    const input = document.getElementById("message-content");
    const status = document.getElementById("message-status");
    const content = input.value.trim();
    if (content === "") {
        status.textContent = "Please enter a message.";
        return;
    }
    try {
        const response =
            await fetch(
                "/api/messages/" + userId,
                {
                    method: "POST",
                    headers:
                        getHeaders(),
                    body:
                        JSON.stringify({content: content})
                }
            );
        if (!response.ok) {
            throw new Error("Could not send message");
        }
        input.value = "";
        status.textContent = "Message sent.";
        loadMessages();
    } catch (error) {
        console.log(error);
        status.textContent = "Message could not be sent.";
    }
}

function getHeaders() {
    const headers = {
        "Content-Type":
            "application/json"
    };

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}