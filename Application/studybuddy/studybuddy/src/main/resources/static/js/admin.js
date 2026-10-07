const adminStatus = document.getElementById("admin-status");
const courseDialog = document.getElementById("course-dialog");
let adminBusy = false;
async function adminChange(url, method, data) {
    if (adminBusy) return;
    adminBusy = true;
    document.querySelectorAll("main button").forEach(button => button.disabled = true);
    try {
        const headers = {"Content-Type": "application/json"};
        headers[document.querySelector('meta[name="_csrf_header"]').content] = document.querySelector('meta[name="_csrf"]').content;
        const response = await fetch(url, {method, headers, credentials: "same-origin", body: data === undefined ? undefined : JSON.stringify(data)});
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(response.status === 401 ? "Your session has ended. Log in again." :
                [body.message || "Unable to complete this operation.", ...Object.values(body.fieldErrors || {})].join(" "));
        }
        sessionStorage.setItem("admin-result", "Change saved successfully.");
        location.reload();
    } catch (error) {
        adminStatus.textContent = error.message; adminStatus.hidden = false;
        if (courseDialog.open) courseDialog.querySelector("[data-dialog-status]").textContent = error.message;
        else adminStatus.scrollIntoView({behavior:"smooth",block:"center"});
    } finally {
        adminBusy = false;
        document.querySelectorAll("main button").forEach(button => button.disabled = false);
    }
}
const previous = sessionStorage.getItem("admin-result");
if (previous) { adminStatus.textContent = previous; adminStatus.hidden = false; sessionStorage.removeItem("admin-result"); }
document.querySelector("[data-course-create]").addEventListener("submit", event => {
    event.preventDefault(); adminChange("/api/courses", "POST", Object.fromEntries(new FormData(event.target)));
});
document.querySelectorAll("[data-edit-course]").forEach(button => button.addEventListener("click", () => {
    const form = courseDialog.querySelector("form");
    form.elements.id.value = button.dataset.id; form.elements.courseCode.value = button.dataset.code; form.elements.courseName.value = button.dataset.name;
    form.querySelector("[data-dialog-status]").textContent = ""; courseDialog.showModal();
}));
document.querySelector("[data-close-dialog]").addEventListener("click", () => courseDialog.close());
document.querySelector("[data-course-edit]").addEventListener("submit", event => {
    event.preventDefault(); const data = Object.fromEntries(new FormData(event.target));
    adminChange(`/api/courses/${data.id}`, "PUT", {courseCode:data.courseCode, courseName:data.courseName});
});
document.querySelectorAll("[data-delete-course]").forEach(button => button.addEventListener("click", () => {
    if(confirm(`Permanently delete course ${button.dataset.name}? This cannot be undone.`)) adminChange(`/api/courses/${button.dataset.id}`, "DELETE");
}));
document.querySelectorAll("[data-user-status]").forEach(button => button.addEventListener("click", () => {
    const enabled = button.dataset.enabled === "true";
    if(confirm(`${enabled ? "Restore" : "Disable"} ${button.dataset.name}?`)) adminChange(`/api/admin/users/${button.dataset.id}/status`, "PUT", {enabled});
}));
document.querySelectorAll("[data-delete-user]").forEach(button => button.addEventListener("click", () => {
    if(confirm(`Permanently delete ${button.dataset.name}? This cannot be undone. Accounts with linked records cannot be deleted.`)) adminChange(`/api/admin/users/${button.dataset.id}`, "DELETE");
}));
document.querySelectorAll("[data-add-member]").forEach(form => form.addEventListener("submit", event => {
    event.preventDefault(); adminChange(`/api/admin/groups/${form.dataset.group}/members/${form.elements.userId.value}`, "POST");
}));
document.querySelectorAll("[data-remove-member]").forEach(button => button.addEventListener("click", () => {
    if(confirm("Remove this member from the group? Historical posts will remain.")) adminChange(`/api/admin/groups/${button.dataset.group}/members/${button.dataset.user}`, "DELETE");
}));
document.querySelectorAll("[data-move-member]").forEach(form => form.addEventListener("submit", event => {
    event.preventDefault();
    if(confirm("Move this member to the selected group? Historical posts will stay in the original group.")) adminChange(`/api/admin/groups/${form.dataset.group}/members/${form.dataset.user}/move`, "POST", {destinationGroupId:Number(form.elements.destinationGroupId.value)});
}));
document.getElementById("user-search").addEventListener("input", event => {
    const query = event.target.value.toLowerCase().trim();
    document.querySelectorAll("[data-user-row]").forEach(row => row.hidden = !row.textContent.toLowerCase().includes(query));
});
