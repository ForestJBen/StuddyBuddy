let currentPlan = null;
let currentPlanRequest = null;
document.addEventListener("DOMContentLoaded", function () {

    const planForm =document.querySelector("[data-plan-form]");

    const summaryForm = document.querySelector("[data-summary-form]");

    const translateGoalButton = document.querySelector("[data-translate-goal]");

    const translateNotesButton = document.querySelector("[data-translate-notes]");
    if (planForm) {
        planForm.addEventListener(
            "submit",
            generatePlan
        );
    }
    if (translateGoalButton) {
        translateGoalButton.addEventListener(
            "click",
            translateGoal
        );
    }
    if (summaryForm) {
        summaryForm.addEventListener(
            "submit",
            generateSummary
        );
    }
    if (translateNotesButton) {
        translateNotesButton.addEventListener(
            "click",
            translateNotes
        );
    }
});

async function translateGoal() {
    const goal = document.getElementById("goal");
    const status = document.querySelector("[data-goal-translation-status]");
    if (goal.value.trim() === "") {
        status.textContent = "Please enter a goal first.";
        return;
    }
    status.textContent = "Translating...";
    try {
        const response = await fetch("/api/ai/translate", {
            method: "POST",
            headers: getHeaders(),
            body: JSON.stringify({
                text: goal.value.trim()
            })
        });
        if (!response.ok) {throw new Error("Translation failed");}
        const result = await response.json();
        goal.value = result.translatedText;
        status.textContent = "Translated to English.";
    } catch (error) {
        console.log(error);
        status.textContent = "Translation failed.";
    }
}
async function translateNotes() {
    const notes = document.getElementById("notes");
    const status = document.querySelector("[data-notes-translation-status]");
    if (notes.value.trim() === "") {
        status.textContent = "Please enter session notes first.";
        return;
    }
    status.textContent = "Translating...";
    try {
        const response = await fetch("/api/ai/translate", {
                method: "POST",
                headers: getHeaders(),
                body: JSON.stringify({
                    text: notes.value.trim()
                })
            });
        if (!response.ok) {throw new Error("Translation failed");}
        const result = await response.json();
        notes.value = result.translatedText;
        status.textContent = "Translated to English.";
    } catch (error) {
        console.log(error);
        status.textContent = "Translation failed.";
    }
}
async function generateSummary(event) {
    event.preventDefault();
    const form = event.target;
    const status =
        document.querySelector("[data-summary-status]");
    if (currentPlan === null ||
        currentPlanRequest === null) {
        status.textContent = "Please generate a study plan first.";
        return;
    }
    const request= {
        course: currentPlanRequest.course,
        topic: currentPlanRequest.topic,
        goal: currentPlanRequest.goal,
        agenda: currentPlan.agenda,
        checklist: currentPlan.checklist,
        notes: form.notes.value.trim()
    };
    status.textContent = "Generating session summary...";

    try {
        const response = await fetch(
            "/api/ai/session-summary",
            {
                    method: "POST",
                    headers: getHeaders(),
                    body:
                        JSON.stringify(request)
                }
            );
        if (!response.ok) {
            throw new Error("Summary generation failed");
        }
        const summary = await response.json();
        showSummary(summary);
        status.textContent = "Session summary generated successfully.";
    } catch (error) {
        console.log(error);
        status.textContent = "Could not generate session summary.";
    }
}
async function generatePlan(event) {

    event.preventDefault();

    const form = event.target;
    const status = document.querySelector("[data-plan-status]");

    const request = {
        course: form.course.value.trim(),
        topic: form.topic.value.trim(),
        goal: form.goal.value.trim(),
        studyStyle: form.studyStyle.value,
        groupSize: Number(form.groupSize.value),
        duration: Number(form.duration.value)
    };

    status.textContent = "Generating study plan...";

    try {

        const response = await fetch("/api/ai/session-plan", {
            method: "POST",
            headers: getHeaders(),
            body: JSON.stringify(request)
        });

        if (!response.ok) {
            throw new Error("Plan generation failed");
        }

        const plan = await response.json();

        currentPlan = plan;
        currentPlanRequest = request;

        showPlan(plan);

        status.textContent = "Study plan generated successfully.";

    } catch (error) {

        console.log(error);
        status.textContent = "Could not generate study plan.";
    }
}

function showPlan(plan) {

    document.getElementById("plan-icebreaker").textContent =
        plan.icebreaker;

    showList("plan-prompts", plan.discussionPrompts);
    showList("plan-agenda", plan.agenda);
    showList("plan-checklist", plan.checklist);

    document.getElementById("plan-result").hidden = false;
}

function showList(id, items) {

    const list = document.getElementById(id);

    list.innerHTML = "";

    items.forEach(function (item) {

        const li = document.createElement("li");

        li.textContent = item;

        list.appendChild(li);
    });
}

function showSummary(summary) {
    document
        .getElementById("summary-overview")
        .textContent =
        summary.sessionOverview;
    showList(
        "summary-topics",
        summary.topicsCompleted
    );
    showList(
        "summary-points",
        summary.keyDiscussionPoints
    );
    showList(
        "summary-not-completed",
        summary.plannedButNotCompleted
    );
    showList(
        "summary-problems",
        summary.remainingProblems
    );
    showList(
        "summary-tasks",
        summary.followUpTasks
    );
    document
        .getElementById("summary-next-goal")
        .textContent =
        summary.nextSessionGoal;
    document
        .getElementById("summary-result")
        .hidden = false;
}

function getHeaders() {

    const headers = {
        "Content-Type": "application/json"
    };

    const csrfToken =
        document.querySelector('meta[name="_csrf"]')?.content;

    const csrfHeader =
        document.querySelector('meta[name="_csrf_header"]')?.content;

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}