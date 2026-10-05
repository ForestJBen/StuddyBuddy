// Groups uses the existing session login, CSRF token and Groups API.
// Keep drafts per group so switching details does not lose an unsent message.
let currentUser = null;
let groups = [];
let selectedGroupId = null;
let writePending = false;
let groupsLoading = false;
let groupsRequest = 0;
let membersRequest = 0;
let postsRequest = 0;
let managedGroupId = null;
const postDrafts = new Map();
const groupElement = id => document.getElementById(id);

function showGroupStatus(id, message, state = 'info') {
    const node = groupElement(id);
    node.textContent = message;
    node.classList.add('group-feedback');
    node.dataset.state = state;
    node.setAttribute('role', state === 'error' ? 'alert' : 'status');
}

function showGroupError(id, error) {
    showGroupStatus(id, error.message, 'error');
    if (error.status === 401) {
        const login = document.createElement('a');
        login.href = '/login'; login.textContent = ' Log in again';
        groupElement(id).append(login);
    }
}

async function groupRequest(url, method = 'GET', body) {
    const headers = {Accept: 'application/json'};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const csrf = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (csrf && csrfHeader) headers[csrfHeader] = csrf;
    let response;
    try {
        response = await fetch(url, {
            method, headers, credentials: 'same-origin',
            body: body === undefined ? undefined : JSON.stringify(body),
            signal: AbortSignal.timeout(20000)
        });
    } catch (error) {
        throw new Error(method === 'GET' ? 'Could not connect. Please use Refresh to try again.' :
            'The result could not be confirmed. Your input is kept. Refresh the list before retrying to avoid duplicates.');
    }
    const data = response.headers.get('content-type')?.includes('json') ? await response.json() : null;
    if (!response.ok) {
        let message = data?.message || 'The request failed. Please refresh and try again.';
        if (response.status === 401) message = 'Your login has expired.';
        if (response.status === 403) message = 'This action is not permitted. Refresh the page to check your login and membership.';
        if (response.status === 405 || response.status === 501) message = 'This action is not available yet. No changes were saved. Please try again after the service is updated.';
        if (response.status === 404) message = 'This group or service is unavailable. Refresh the groups list.';
        if (response.status >= 500) message = 'The groups service is temporarily unavailable. Please try again later.';
        if (data?.fieldErrors) message += ' ' + Object.values(data.fieldErrors).join(' ');
        const error = new Error(message); error.status = response.status; throw error;
    }
    if (response.status === 204) return null;
    if (data === null) throw new Error('Unexpected server response. Please refresh the page.');
    return data;
}

function groupText(tag, text, className = '') {
    const node = document.createElement(tag);
    node.textContent = text; node.className = className; return node;
}

function groupDate(value) {
    const date = new Date(value);
    return value && Number.isFinite(date.getTime()) ? date.toLocaleString() : 'Not available';
}

function selectedGroup() {
    return groups.find(group => group.id === selectedGroupId);
}

function groupInfo(group, node) {
    node.append(
        groupText('p', group.course, 'group-badge'),
        groupText('p', group.description, 'group-description'),
        groupText('p', `Meeting: ${group.meetingTime || 'Not arranged'} · Location: ${group.location || 'Not arranged'}`),
        groupText('p', `Created by ${group.creatorName} · ${group.memberCount} ${group.memberCount === 1 ? 'member' : 'members'}`, 'group-meta'),
        groupText('p', `Created ${groupDate(group.createdAt)}`, 'group-meta')
    );
}

function renderGroups() {
    const list = groupElement('groups-list'); list.replaceChildren();
    groups.forEach(group => {
        const card = groupText('article', '', 'group-card');
        card.append(groupText('h3', group.name)); groupInfo(group, card);
        const actions = groupText('div', '', 'group-actions');
        const open = groupText('button', 'Open group', 'button-secondary');
        open.type = 'button'; open.disabled = groupsLoading;
        open.addEventListener('click', () => openGroup(group.id)); actions.append(open);
        if (currentUser && group.creatorId === currentUser.id) {
            actions.append(groupText('span', 'Creator · You cannot leave your own group', 'group-badge'));
            const edit = groupText('button', 'Edit group', 'button-secondary');
            const remove = groupText('button', 'Delete group', 'button-secondary group-danger');
            edit.type = remove.type = 'button';
            edit.disabled = remove.disabled = writePending || groupsLoading;
            edit.addEventListener('click', () => openGroupEditor(group));
            remove.addEventListener('click', () => openGroupDelete(group));
            actions.append(edit, remove);
        } else if (currentUser) {
            const membership = groupText('button', group.joined ? 'Leave group' : 'Join group', 'button-secondary');
            membership.type = 'button'; membership.disabled = writePending || groupsLoading;
            membership.addEventListener('click', () => changeMembership(group)); actions.append(membership);
        }
        card.append(actions); list.append(card);
    });
}

function updatePostForm() {
    const group = selectedGroup();
    const canPost = Boolean(currentUser && group?.joined);
    groupElement('group-post-form').hidden = !canPost;
    groupElement('post-access-note').textContent = canPost ? 'Messages are visible to all logged-in students.' : 'Join this group to post a message. You can still read the conversation.';
    groupElement('group-post-form').querySelector('button').disabled = writePending || groupsLoading;
    groupElement('group-post-content').value = postDrafts.get(selectedGroupId) || '';
}

// Refresh the list after writes. A newer request supersedes an older one.
async function loadGroups() {
    const version = ++groupsRequest; groupsLoading = true;
    groupElement('refresh-groups').disabled = true; renderGroups(); updatePostForm();
    showGroupStatus('groups-status', 'Loading groups…');
    try {
        if (!currentUser) currentUser = await groupRequest('/api/auth/me');
        const result = await groupRequest('/api/groups');
        if (version !== groupsRequest) return;
        groups = result;
        showGroupStatus('groups-status', groups.length ? `${groups.length} ${groups.length === 1 ? 'group' : 'groups'} available.` : 'No groups available yet. Create the first one above.');
        if (selectedGroupId !== null) {
            if (selectedGroup()) renderGroupDetails();
            else closeGroup();
        }
    } catch (error) {
        if (version !== groupsRequest) return;
        groups = []; closeGroup(); showGroupError('groups-status', error);
    } finally {
        if (version === groupsRequest) {
            groupsLoading = false; groupElement('refresh-groups').disabled = false;
            renderGroups(); updatePostForm();
        }
    }
}

function renderGroupDetails() {
    const group = selectedGroup(); if (!group) return;
    groupElement('group-detail-title').textContent = group.name;
    groupElement('group-detail-info').replaceChildren(); groupInfo(group, groupElement('group-detail-info'));
    updatePostForm();
}

function openGroup(id) {
    selectedGroupId = id; groupElement('group-detail').hidden = false;
    showGroupStatus('post-form-status', ''); renderGroupDetails();
    loadMembers(); loadPosts();
    groupElement('group-detail-title').focus({preventScroll: true});
    groupElement('group-detail').scrollIntoView({behavior: 'smooth', block: 'start'});
}

function closeGroup() {
    selectedGroupId = null; membersRequest++; postsRequest++;
    groupElement('group-detail').hidden = true;
}

async function loadMembers() {
    const id = selectedGroupId; if (id === null) return;
    const version = ++membersRequest;
    groupElement('group-members').replaceChildren(); groupElement('refresh-members').disabled = true;
    showGroupStatus('members-status', 'Loading members…');
    try {
        const members = await groupRequest(`/api/groups/${id}/members`);
        if (version !== membersRequest) return;
        members.forEach(member => groupElement('group-members').append(groupText('li', `${member.firstName} ${member.lastName}${member.creator ? ' · Creator' : ''}`)));
        showGroupStatus('members-status', members.length ? '' : 'No members to display.');
    } catch (error) { if (version === membersRequest) showGroupError('members-status', error); }
    finally { if (version === membersRequest) groupElement('refresh-members').disabled = false; }
}

async function loadPosts() {
    const id = selectedGroupId; if (id === null) return;
    const version = ++postsRequest;
    groupElement('group-posts').replaceChildren(); groupElement('refresh-posts').disabled = true;
    showGroupStatus('posts-status', 'Loading posts…');
    try {
        const posts = await groupRequest(`/api/groups/${id}/posts`);
        if (version !== postsRequest) return;
        posts.forEach(post => {
            const article = groupText('article', '', 'group-post');
            article.append(groupText('strong', post.authorName), groupText('p', groupDate(post.createdAt), 'group-meta'), groupText('p', post.content, 'group-post-content'));
            groupElement('group-posts').append(article);
        });
        showGroupStatus('posts-status', posts.length ? '' : 'No posts yet. Members can start the conversation below.');
    } catch (error) { if (version === postsRequest) showGroupError('posts-status', error); }
    finally { if (version === postsRequest) groupElement('refresh-posts').disabled = false; }
}

// Only one write at a time; GET requests remain available for refresh/navigation.
function setGroupWritePending(value) {
    writePending = value;
    groupElement('create-group-form').querySelector('button[type="submit"]').disabled = value;
    renderGroups(); updatePostForm();
}

async function createGroup(event) {
    event.preventDefault(); if (writePending) return;
    const form = event.target;
    const body = Object.fromEntries(Array.from(new FormData(form), ([key, value]) => [key, value.trim()]));
    if (!body.name || !body.course || !body.description) {
        showGroupStatus('group-form-status', 'Enter a group name, course and description.', 'error'); return;
    }
    setGroupWritePending(true); showGroupStatus('group-form-status', 'Creating group…');
    form.querySelectorAll('input, textarea').forEach(field => field.readOnly = true);
    try {
        await groupRequest('/api/groups', 'POST', body);
        form.reset(); showGroupStatus('group-form-status', 'Group created. You have automatically joined as its creator.', 'success');
        await loadGroups();
    } catch (error) { showGroupError('group-form-status', error); }
    finally { form.querySelectorAll('input, textarea').forEach(field => field.readOnly = false); setGroupWritePending(false); }
}

async function changeMembership(group) {
    if (writePending) return;
    setGroupWritePending(true); showGroupStatus('group-action-status', group.joined ? 'Leaving group…' : 'Joining group…');
    try {
        const updated = await groupRequest(`/api/groups/${group.id}/${group.joined ? 'leave' : 'join'}`, group.joined ? 'DELETE' : 'POST');
        groups = groups.map(item => item.id === updated.id ? updated : item);
        showGroupStatus('group-action-status', group.joined ? `You left ${group.name}.` : `You joined ${group.name}.`, 'success');
        await loadGroups();
        if (selectedGroupId === group.id) await loadMembers();
    } catch (error) {
        showGroupError('group-action-status', error);
        if ([403, 404, 409].includes(error.status)) await loadGroups();
    } finally { setGroupWritePending(false); }
}

async function createGroupPost(event) {
    event.preventDefault(); if (writePending || !selectedGroup()?.joined) return;
    const id = selectedGroupId;
    const content = groupElement('group-post-content').value.trim();
    if (!content) { showGroupStatus('post-form-status', 'Enter a message before posting.', 'error'); return; }
    const originalDraft = postDrafts.get(id);
    setGroupWritePending(true); showGroupStatus('post-form-status', 'Posting message…');
    try {
        await groupRequest(`/api/groups/${id}/posts`, 'POST', {content});
        if (postDrafts.get(id) === originalDraft) postDrafts.delete(id);
        if (selectedGroupId === id) {
            updatePostForm(); showGroupStatus('post-form-status', 'Message posted.', 'success'); await loadPosts();
        } else showGroupStatus('group-action-status', 'Your message was posted to the previous group.', 'success');
    } catch (error) {
        showGroupError(selectedGroupId === id ? 'post-form-status' : 'group-action-status', error);
        if (error.status === 403) await loadGroups();
    } finally { setGroupWritePending(false); }
}

// The backend must independently check creator ownership for both endpoints.
function openGroupDialog(id) {
    const dialog = groupElement(id);
    dialog.showModal(); dialog.querySelector('h2').focus(); dialog.scrollTop = 0;
}

function openGroupEditor(group) {
    if (writePending || group.creatorId !== currentUser?.id) return;
    managedGroupId = group.id;
    const form = groupElement('edit-group-form');
    for (const field of ['name', 'course', 'description', 'meetingTime', 'location']) {
        form.elements[field].value = group[field] || '';
    }
    showGroupStatus('edit-group-status', ''); openGroupDialog('edit-group-dialog');
}

function openGroupDelete(group) {
    if (writePending || group.creatorId !== currentUser?.id) return;
    managedGroupId = group.id;
    groupElement('delete-group-form').reset();
    groupElement('delete-group-name').textContent = `Delete “${group.name}”?`;
    showGroupStatus('delete-group-status', ''); openGroupDialog('delete-group-dialog');
}

function lockGroupDialog(form, locked) {
    form.querySelectorAll('button, input, textarea').forEach(control => control.disabled = locked);
    form.setAttribute('aria-busy', String(locked));
}

async function saveGroupEdit(event) {
    event.preventDefault(); if (writePending) return;
    const form = event.target;
    const id = managedGroupId;
    const body = Object.fromEntries(Array.from(new FormData(form), ([key, value]) => [key, value.trim()]));
    if (!body.name || !body.course || !body.description) {
        showGroupStatus('edit-group-status', 'Enter a group name, course and description.', 'error'); return;
    }
    setGroupWritePending(true); lockGroupDialog(form, true);
    showGroupStatus('edit-group-status', 'Saving changes…');
    try {
        const updated = await groupRequest(`/api/groups/${id}`, 'PUT', body);
        if (!updated || updated.id !== id) throw new Error('The save result could not be confirmed. Refresh the groups list before trying again.');
        // Invalidate older list requests so they cannot restore stale content.
        groupsRequest++; groupsLoading = false; groupElement('refresh-groups').disabled = false;
        groups = groups.map(group => group.id === id ? updated : group);
        if (selectedGroupId === id) renderGroupDetails();
        groupElement('edit-group-dialog').close();
        showGroupStatus('group-action-status', 'Group updated successfully.', 'success');
        await loadGroups();
    } catch (error) { showGroupError('edit-group-status', error); }
    finally { lockGroupDialog(form, false); setGroupWritePending(false); }
}

async function deleteGroup(event) {
    event.preventDefault(); if (writePending || !groupElement('confirm-group-delete').checked) return;
    const form = event.target; const id = managedGroupId;
    setGroupWritePending(true); lockGroupDialog(form, true);
    showGroupStatus('delete-group-status', 'Deleting group…');
    try {
        await groupRequest(`/api/groups/${id}`, 'DELETE');
        groupsRequest++; groupsLoading = false; groupElement('refresh-groups').disabled = false;
        groups = groups.filter(group => group.id !== id); postDrafts.delete(id);
        if (selectedGroupId === id) closeGroup();
        groupElement('delete-group-dialog').close();
        showGroupStatus('group-action-status', 'Group deleted successfully.', 'success');
        await loadGroups();
    } catch (error) { showGroupError('delete-group-status', error); }
    finally { lockGroupDialog(form, false); setGroupWritePending(false); }
}

document.addEventListener('DOMContentLoaded', () => {
    groupElement('edit-group-form').addEventListener('submit', saveGroupEdit);
    groupElement('delete-group-form').addEventListener('submit', deleteGroup);
    document.querySelectorAll('[data-close-group-dialog]').forEach(button => {
        button.addEventListener('click', () => { if (!writePending) button.closest('dialog').close(); });
    });
    document.querySelectorAll('.groups-page dialog').forEach(dialog => {
        dialog.addEventListener('cancel', event => { if (writePending) event.preventDefault(); });
        dialog.addEventListener('close', () => groupElement('refresh-groups').focus());
    });
    groupElement('create-group-form').addEventListener('submit', createGroup);
    groupElement('group-post-form').addEventListener('submit', createGroupPost);
    groupElement('group-post-content').addEventListener('input', event => postDrafts.set(selectedGroupId, event.target.value));
    groupElement('refresh-groups').addEventListener('click', loadGroups);
    groupElement('refresh-members').addEventListener('click', loadMembers);
    groupElement('refresh-posts').addEventListener('click', loadPosts);
    groupElement('close-group').addEventListener('click', closeGroup);
    loadGroups();
});
