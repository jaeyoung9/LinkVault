// ===== Comment System =====

var COLLAPSE_THRESHOLD = 3;
var MAX_VISUAL_DEPTH = 2;
var expandedThreads = {};

function loadComments() {
    fetch('/api/comments/bookmark/' + bookmarkId)
        .then(function(r) { return r.json(); })
        .then(function(comments) {
            var container = document.getElementById('commentsContainer');
            var noComments = document.getElementById('noComments');

            if (comments.length === 0) {
                container.innerHTML = '';
                container.appendChild(noComments);
                noComments.style.display = '';
                return;
            }

            container.innerHTML = '';
            comments.forEach(function(comment) {
                container.appendChild(renderComment(comment, 0));
            });
        })
        .catch(function() {
            showToast('Error loading comments', 'error');
        });
}

function renderComment(comment, visualDepth) {
    var wrapper = document.createElement('div');

    // Build the comment item
    var div = document.createElement('div');
    div.className = 'comment-item';
    div.dataset.id = comment.id;

    var likeActive = comment.userVote === 'LIKE' ? ' vote-active' : '';
    var dislikeActive = comment.userVote === 'DISLIKE' ? ' vote-active' : '';

    // Avatar (first letter of username)
    var avatarLetter = comment.deleted ? '?' : comment.username.charAt(0).toUpperCase();
    var contentClass = comment.deleted ? 'comment-content deleted-content' : 'comment-content';

    // Reply-to label for depth > MAX_VISUAL_DEPTH (flattened replies)
    var replyToHtml = '';
    if (comment.parentUsername && visualDepth >= MAX_VISUAL_DEPTH) {
        var parentDisplay = comment.parentUsername === '[deleted]'
            ? '<em>[deleted]</em>'
            : '<a href="javascript:void(0)" onclick="scrollToComment(' + comment.parentId + ')">@' + escapeHtml(comment.parentUsername) + '</a>';
        replyToHtml = '<div class="comment-reply-to">&#8627; replying to ' + parentDisplay + '</div>';
    }

    div.innerHTML =
        '<div class="comment-header">' +
            '<span class="comment-avatar">' + avatarLetter + '</span>' +
            '<strong>' + escapeHtml(comment.deleted ? '[deleted]' : comment.username) + '</strong>' +
            '<span class="comment-time">' + formatTime(comment.createdAt) + '</span>' +
            (comment.edited ? '<span class="comment-edited">(edited)</span>' : '') +
        '</div>' +
        replyToHtml +
        '<div class="' + contentClass + '" id="content-' + comment.id + '">' + escapeHtml(comment.content) + '</div>' +
        (!comment.deleted ?
            '<div class="comment-actions">' +
                '<button class="vote-btn' + likeActive + '" onclick="voteComment(' + comment.id + ', \'LIKE\')">' +
                    '&#9650; ' + comment.likeCount +
                '</button>' +
                '<button class="vote-btn' + dislikeActive + '" onclick="voteComment(' + comment.id + ', \'DISLIKE\')">' +
                    '&#9660; ' + comment.dislikeCount +
                '</button>' +
                '<span style="color:var(--border);">|</span>' +
                (comment.depth < 5 ? '<button class="comment-action-btn" onclick="toggleReplyForm(' + comment.id + ', \'' + escapeAttr(comment.username) + '\')">Reply</button>' : '') +
                (comment.canEdit ? '<button class="comment-action-btn" onclick="editComment(' + comment.id + ')">Edit</button>' : '') +
                (comment.canDelete ? '<button class="comment-action-btn" onclick="deleteComment(' + comment.id + ')">Delete</button>' : '') +
            '</div>'
            : '') +
        '<div class="reply-form" id="reply-form-' + comment.id + '" style="display:none;">' +
            '<div class="reply-form-label">' +
                'Replying to <strong>' + escapeHtml(comment.username) + '</strong>' +
                '<span class="reply-form-close" onclick="toggleReplyForm(' + comment.id + ', \'' + escapeAttr(comment.username) + '\')">&times;</span>' +
            '</div>' +
            '<textarea class="form-control" id="reply-content-' + comment.id + '" placeholder="Write a reply..." rows="2"></textarea>' +
            '<div style="margin-top:6px;text-align:right;">' +
                '<button class="btn btn-sm btn-outline" onclick="toggleReplyForm(' + comment.id + ', \'' + escapeAttr(comment.username) + '\')">Cancel</button> ' +
                '<button class="btn btn-sm btn-primary" onclick="submitReply(' + comment.id + ', ' + comment.bookmarkId + ')">Reply</button>' +
            '</div>' +
        '</div>';

    wrapper.appendChild(div);

    // Render replies
    if (comment.replies && comment.replies.length > 0) {
        var threadDiv = document.createElement('div');
        // Cap visual nesting: after MAX_VISUAL_DEPTH, stop adding thread containers
        var nextVisualDepth = visualDepth + 1;
        if (nextVisualDepth <= MAX_VISUAL_DEPTH) {
            threadDiv.className = 'comment-thread';
        }
        threadDiv.id = 'thread-' + comment.id;

        var shouldCollapse = comment.replies.length > COLLAPSE_THRESHOLD && !expandedThreads[comment.id];

        if (shouldCollapse) {
            // Show last 2 as preview
            var previewCount = 2;
            var hidden = comment.replies.length - previewCount;

            // Expand button
            var expandBtn = document.createElement('button');
            expandBtn.className = 'comment-collapse-btn';
            expandBtn.textContent = 'View ' + hidden + ' more ' + (hidden === 1 ? 'reply' : 'replies');
            expandBtn.onclick = (function(cid) {
                return function() {
                    expandedThreads[cid] = true;
                    loadComments();
                };
            })(comment.id);
            threadDiv.appendChild(expandBtn);

            // Render only the last 2
            var previewReplies = comment.replies.slice(-previewCount);
            previewReplies.forEach(function(reply) {
                threadDiv.appendChild(renderComment(reply, nextVisualDepth));
            });
        } else {
            // Render all replies
            comment.replies.forEach(function(reply) {
                threadDiv.appendChild(renderComment(reply, nextVisualDepth));
            });

            // Collapse button if over threshold
            if (comment.replies.length > COLLAPSE_THRESHOLD) {
                var collapseBtn = document.createElement('button');
                collapseBtn.className = 'comment-collapse-btn';
                collapseBtn.textContent = 'Hide replies';
                collapseBtn.onclick = (function(cid) {
                    return function() {
                        delete expandedThreads[cid];
                        loadComments();
                    };
                })(comment.id);
                threadDiv.appendChild(collapseBtn);
            }
        }

        wrapper.appendChild(threadDiv);
    }

    return wrapper;
}

function scrollToComment(commentId) {
    var el = document.querySelector('[data-id="' + commentId + '"]');
    if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        el.style.transition = 'background 0.3s';
        el.style.background = 'rgba(59,130,246,0.15)';
        setTimeout(function() { el.style.background = ''; }, 1500);
    }
}

function submitComment() {
    var content = document.getElementById('newCommentContent').value.trim();
    if (!content) {
        showToast('Please enter a comment', 'error');
        return;
    }

    fetch('/api/comments', {
        method: 'POST',
        headers: csrfHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ content: content, bookmarkId: bookmarkId })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        document.getElementById('newCommentContent').value = '';
        showToast('Comment posted');
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error posting comment', 'error');
    });
}

function submitReply(parentId, bmId) {
    var content = document.getElementById('reply-content-' + parentId).value.trim();
    if (!content) {
        showToast('Please enter a reply', 'error');
        return;
    }

    fetch('/api/comments', {
        method: 'POST',
        headers: csrfHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ content: content, bookmarkId: bmId, parentId: parentId })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        showToast('Reply posted');
        // Keep thread expanded after posting
        expandedThreads[parentId] = true;
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error posting reply', 'error');
    });
}

function editComment(commentId) {
    var contentEl = document.getElementById('content-' + commentId);
    var currentText = contentEl.textContent;

    contentEl.innerHTML =
        '<textarea class="form-control" id="edit-content-' + commentId + '" rows="2">' + escapeHtml(currentText) + '</textarea>' +
        '<div style="margin-top:6px;text-align:right;">' +
            '<button class="btn btn-sm btn-outline" onclick="loadComments()">Cancel</button> ' +
            '<button class="btn btn-sm btn-primary" onclick="saveEdit(' + commentId + ')">Save</button>' +
        '</div>';

    document.getElementById('edit-content-' + commentId).focus();
}

function saveEdit(commentId) {
    var content = document.getElementById('edit-content-' + commentId).value.trim();
    if (!content) return;

    fetch('/api/comments/' + commentId, {
        method: 'PUT',
        headers: csrfHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ content: content })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        showToast('Comment updated');
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error updating comment', 'error');
    });
}

function deleteComment(commentId) {
    if (!confirm('Delete this comment?')) return;

    fetch('/api/comments/' + commentId, { method: 'DELETE', headers: csrfHeaders() })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        showToast('Comment deleted');
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error deleting comment', 'error');
    });
}

function voteComment(commentId, voteType) {
    fetch('/api/comments/' + commentId + '/vote', {
        method: 'POST',
        headers: csrfHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ voteType: voteType })
    })
    .then(function(r) {
        if (!r.ok) return r.json().then(function(e) { throw e; });
        return r.json();
    })
    .then(function() {
        loadComments();
    })
    .catch(function(err) {
        showToast(err.message || 'Error voting', 'error');
    });
}

function toggleReplyForm(commentId, username) {
    var form = document.getElementById('reply-form-' + commentId);
    var isHidden = form.style.display === 'none';
    form.style.display = isHidden ? '' : 'none';

    if (isHidden) {
        var textarea = document.getElementById('reply-content-' + commentId);
        if (textarea.value === '' && username) {
            textarea.value = '@' + username + ' ';
        }
        textarea.focus();
    }
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    var d = new Date(dateStr);
    var now = new Date();
    var diff = Math.floor((now - d) / 1000);

    if (diff < 60) return 'just now';
    if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
    if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
    if (diff < 604800) return Math.floor(diff / 86400) + 'd ago';
    return d.toLocaleDateString();
}

function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function escapeAttr(text) {
    return text.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

// Load comments on page ready
document.addEventListener('DOMContentLoaded', loadComments);
