document.addEventListener("DOMContentLoaded", function () {loadMatches();});

async function loadMatches() {

    const status = document.querySelector("[data-matches-status]");

    const list = document.querySelector("[data-matches-list]");

    try {
        const response = await fetch("/api/recommendations");

        if (!response.ok) {throw new Error("Could not load recommendations");}

        const recommendations = await response.json();

        if (recommendations.length === 0) {
            status.textContent = "No matching study buddies are available yet.";
            return;
        }
        status.textContent = recommendations.length + " recommended study buddies found.";

        recommendations.forEach(function (recommendation) {
            const card = createMatchCard(recommendation);
            list.appendChild(card);
        });

    } catch (error) {
        console.log(error);
        status.textContent = "Recommendations could not be loaded.";
    }
}

function createMatchCard(recommendation) {

    const card = document.createElement("article");

    card.className = "recommendation-card";

    const name = document.createElement("h2");

    name.textContent = recommendation.firstName + " " + recommendation.lastName;

    const score = document.createElement("p");

    score.textContent = recommendation.matchScore.toFixed(1) + "% match";

    const coursesTitle = document.createElement("strong");

    coursesTitle.textContent = "Shared courses";

    const courses = document.createElement("p");

    courses.textContent = recommendation.sharedCourses.join(" · ");

    const reasonsTitle = document.createElement("strong");

    reasonsTitle.textContent = "Why this match?";

    const reasons = document.createElement("ul");

    recommendation.reasons.forEach(function (reason) {
        const item = document.createElement("li");
        item.textContent = reason;
        reasons.appendChild(item);
    });

    card.appendChild(name);
    card.appendChild(score);
    card.appendChild(coursesTitle);
    card.appendChild(courses);
    card.appendChild(reasonsTitle);
    card.appendChild(reasons);

    const messageButton = document.createElement("a");

    messageButton.textContent = "Message";
    messageButton.href = "/messages/" + recommendation.userId;
    messageButton.className = "button";
    card.appendChild(messageButton);
    return card;
}