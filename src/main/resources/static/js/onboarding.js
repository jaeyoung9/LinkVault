// ===== LinkVault Onboarding Tour =====
(function() {
    'use strict';

    // Screen detection from URL path
    function detectScreen() {
        var path = window.location.pathname;
        if (path === '/' || path === '') return 'feed';
        if (path.startsWith('/map')) return 'map';
        if (path.startsWith('/saved')) return 'saved';
        if (path.startsWith('/import')) return 'import';
        if (path.startsWith('/settings')) return 'settings';
        return null;
    }

    // Map targetElement names to actual CSS selectors on the real pages
    var SELECTOR_MAP = {
        feed: {
            'sidebar':       '.sidebar',
            'search-bar':    '.search-form',
            'share-button':  '.header-bar .btn-primary',
            'frequent-bar':  '.frequent-bar',
            'post-grid':     '.post-grid',
            'post-card':     '.post-card'
        },
        map: {
            'sidebar':       '.sidebar',
            'map-area':      '#discoveryMap',
            'map-search':    '#search-wrapper',
            'share-button':  '.header-bar .btn-primary'
        },
        saved: {
            'sidebar':       '.sidebar',
            'tabs':          '.settings-tabs',
            'post-grid':     '.post-grid',
            'new-private-btn': '#privateTab .btn-primary'
        },
        'import': {
            'sidebar':           '.sidebar',
            'import-zone-json':  '.import-zone',
            'import-zone-html':  '.main-content > div > div:nth-child(2) .import-zone'
        },
        settings: {
            'sidebar':        '.sidebar',
            'tabs':           '.settings-tabs',
            'theme-options':  '.theme-options',
            'toggle-row':     '.settings-toggle-row'
        }
    };

    var state = {
        screen: null,
        data: null,
        currentStep: -1,
        overlayEl: null,
        spotlightEl: null,
        tooltipEl: null,
        welcomeEl: null
    };

    // Uses csrfHeaders() from app.js (loaded before init runs via window.load + 500ms delay)

    function escHtml(s) {
        if (!s) return '';
        return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    // Create overlay elements (injected into body)
    function createOverlayElements() {
        // Dark overlay
        var overlay = document.createElement('div');
        overlay.id = 'onboard-overlay';
        overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;z-index:9998;display:none;pointer-events:none;';
        document.body.appendChild(overlay);
        state.overlayEl = overlay;

        // Spotlight cutout
        var spotlight = document.createElement('div');
        spotlight.id = 'onboard-spotlight';
        spotlight.style.cssText = 'position:fixed;z-index:9999;border-radius:6px;pointer-events:none;display:none;' +
            'box-shadow:0 0 0 9999px rgba(0,0,0,0.55);transition:all 0.3s ease;';
        document.body.appendChild(spotlight);
        state.spotlightEl = spotlight;

        // Tooltip
        var tooltip = document.createElement('div');
        tooltip.id = 'onboard-tooltip';
        tooltip.style.cssText = 'position:fixed;z-index:10000;background:#1e1b4b;color:#fff;border-radius:10px;' +
            'padding:16px 18px;font-size:0.85rem;max-width:300px;display:none;' +
            'box-shadow:0 8px 32px rgba(0,0,0,0.35);font-family:system-ui,-apple-system,sans-serif;';
        document.body.appendChild(tooltip);
        state.tooltipEl = tooltip;

        // Welcome modal
        var welcome = document.createElement('div');
        welcome.id = 'onboard-welcome';
        welcome.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;z-index:10001;' +
            'background:rgba(0,0,0,0.6);display:none;align-items:center;justify-content:center;';
        document.body.appendChild(welcome);
        state.welcomeEl = welcome;

        // Inject tooltip CSS
        var style = document.createElement('style');
        style.textContent =
            '#onboard-tooltip .ob-tt-title{font-weight:700;font-size:0.95rem;margin-bottom:6px;}' +
            '#onboard-tooltip .ob-tt-content{color:#c7d2fe;line-height:1.5;margin-bottom:14px;}' +
            '#onboard-tooltip .ob-tt-nav{display:flex;justify-content:space-between;align-items:center;gap:8px;}' +
            '#onboard-tooltip .ob-tt-nav button{padding:6px 14px;border-radius:6px;border:1px solid rgba(255,255,255,0.3);' +
                'background:transparent;color:#fff;font-size:0.8rem;cursor:pointer;transition:background 0.15s;}' +
            '#onboard-tooltip .ob-tt-nav button:hover{background:rgba(255,255,255,0.15);}' +
            '#onboard-tooltip .ob-tt-nav button.ob-primary{background:#6366f1;border-color:#6366f1;}' +
            '#onboard-tooltip .ob-tt-nav button.ob-primary:hover{background:#4f46e5;}' +
            '#onboard-tooltip .ob-tt-indicator{font-size:0.72rem;color:#a5b4fc;text-align:center;margin-top:8px;}' +
            '#onboard-welcome .ob-wm-box{background:#fff;border-radius:16px;padding:36px 32px;max-width:380px;' +
                'text-align:center;box-shadow:0 12px 40px rgba(0,0,0,0.25);color:#111827;}' +
            '#onboard-welcome .ob-wm-box h3{margin:0 0 10px;font-size:1.2rem;}' +
            '#onboard-welcome .ob-wm-box p{font-size:0.9rem;color:#6b7280;margin:0 0 24px;line-height:1.5;}' +
            '#onboard-welcome .ob-wm-box button{padding:10px 28px;border:none;border-radius:8px;' +
                'background:#6366f1;color:#fff;font-size:0.9rem;cursor:pointer;transition:background 0.15s;}' +
            '#onboard-welcome .ob-wm-box button:hover{background:#4f46e5;}' +
            '#onboard-welcome .ob-wm-skip{margin-top:12px;font-size:0.8rem;color:#9ca3af;cursor:pointer;border:none;background:none;}' +
            '#onboard-welcome .ob-wm-skip:hover{color:#6b7280;}';
        document.head.appendChild(style);
    }

    // Show welcome modal
    function showWelcome() {
        var d = state.data;
        state.welcomeEl.innerHTML =
            '<div class="ob-wm-box">' +
                '<h3>' + escHtml(d.welcomeTitle) + '</h3>' +
                '<p>' + escHtml(d.welcomeDescription) + '</p>' +
                '<button onclick="window.__onboardStartTour()">Start Tour</button>' +
                (d.dismissible ? '<br/><button class="ob-wm-skip" onclick="window.__onboardDismiss()">Skip tour</button>' : '') +
            '</div>';
        state.welcomeEl.style.display = 'flex';
    }

    // Start the step-by-step tour
    function startTour() {
        state.welcomeEl.style.display = 'none';
        if (state.data.steps.length === 0) {
            completeTour();
            return;
        }
        state.overlayEl.style.display = 'block';
        showStep(0);
    }

    // Show a specific step
    function showStep(index) {
        var steps = state.data.steps;
        if (index < 0 || index >= steps.length) {
            completeTour();
            return;
        }
        state.currentStep = index;
        var step = steps[index];

        // Find the target element
        var selectorMap = SELECTOR_MAP[state.screen] || {};
        var selector = selectorMap[step.targetElement];
        var target = selector ? document.querySelector(selector) : null;

        if (!target) {
            // If target not found, skip to next step
            if (index < steps.length - 1) {
                showStep(index + 1);
            } else {
                completeTour();
            }
            return;
        }

        // Position spotlight on target
        var rect = target.getBoundingClientRect();
        var pad = 6;
        state.spotlightEl.style.display = 'block';
        state.spotlightEl.style.top = (rect.top - pad) + 'px';
        state.spotlightEl.style.left = (rect.left - pad) + 'px';
        state.spotlightEl.style.width = (rect.width + pad * 2) + 'px';
        state.spotlightEl.style.height = (rect.height + pad * 2) + 'px';

        // Build tooltip content
        var navHtml = '<div class="ob-tt-nav">';
        if (index > 0) {
            navHtml += '<button onclick="window.__onboardPrev()">&#9664; Prev</button>';
        } else {
            navHtml += '<span></span>';
        }
        if (index < steps.length - 1) {
            navHtml += '<button class="ob-primary" onclick="window.__onboardNext()">Next &#9654;</button>';
        } else {
            navHtml += '<button class="ob-primary" onclick="window.__onboardComplete()">Finish</button>';
        }
        navHtml += '</div>';
        navHtml += '<div class="ob-tt-indicator">Step ' + (index + 1) + ' of ' + steps.length + '</div>';

        state.tooltipEl.innerHTML =
            '<div class="ob-tt-title">' + escHtml(step.title) + '</div>' +
            '<div class="ob-tt-content">' + escHtml(step.content) + '</div>' +
            navHtml +
            (state.data.dismissible ?
                '<div style="text-align:right;margin-top:6px;">' +
                    '<button style="border:none;background:none;color:#a5b4fc;font-size:0.72rem;cursor:pointer;padding:0;" onclick="window.__onboardDismiss()">Skip tour</button>' +
                '</div>' : '');

        state.tooltipEl.style.display = 'block';

        // Position tooltip below or above target
        // We need to wait a frame for tooltip dimensions
        requestAnimationFrame(function() {
            positionTooltip(rect);
        });

        // Scroll target into view if needed
        target.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function positionTooltip(targetRect) {
        var tt = state.tooltipEl;
        var ttRect = tt.getBoundingClientRect();
        var viewH = window.innerHeight;
        var viewW = window.innerWidth;

        var top, left;

        // Try below target
        top = targetRect.bottom + 12;
        if (top + ttRect.height > viewH) {
            // Try above
            top = targetRect.top - ttRect.height - 12;
        }
        if (top < 8) top = 8;

        // Horizontal: align with target left, but keep within viewport
        left = targetRect.left;
        if (left + ttRect.width > viewW - 12) {
            left = viewW - ttRect.width - 12;
        }
        if (left < 12) left = 12;

        tt.style.top = top + 'px';
        tt.style.left = left + 'px';
    }

    // Navigation
    function nextStep() {
        showStep(state.currentStep + 1);
    }

    function prevStep() {
        showStep(state.currentStep - 1);
    }

    // Complete the tour
    function completeTour() {
        hideOverlays();
        // Mark as completed on server
        fetch('/api/settings/guidelines/complete', {
            method: 'POST',
            headers: csrfHeaders({'Content-Type': 'application/json'})
        }).catch(function(e) { console.warn('[Onboarding] complete error:', e); });
    }

    // Dismiss (skip) the tour
    function dismissTour() {
        hideOverlays();
        if (state.data && state.data.dismissible) {
            fetch('/api/settings/guidelines/complete', {
                method: 'POST',
                headers: csrfHeaders({'Content-Type': 'application/json'})
            }).catch(function(e) { console.warn('[Onboarding] dismiss error:', e); });
        }
    }

    function hideOverlays() {
        if (state.overlayEl) state.overlayEl.style.display = 'none';
        if (state.spotlightEl) state.spotlightEl.style.display = 'none';
        if (state.tooltipEl) state.tooltipEl.style.display = 'none';
        if (state.welcomeEl) state.welcomeEl.style.display = 'none';
    }

    // Expose functions to window for onclick handlers
    window.__onboardStartTour = startTour;
    window.__onboardNext = nextStep;
    window.__onboardPrev = prevStep;
    window.__onboardComplete = completeTour;
    window.__onboardDismiss = dismissTour;

    // Handle window resize — reposition spotlight/tooltip
    window.addEventListener('resize', function() {
        if (state.currentStep >= 0 && state.data && state.data.steps.length > state.currentStep) {
            showStep(state.currentStep);
        }
    });

    // Main init
    function init() {
        var screen = detectScreen();
        console.log('[Onboarding] init — screen:', screen);
        if (!screen) return; // Not a guideline-enabled page

        state.screen = screen;

        // Fetch onboarding data (GET — no Content-Type needed)
        fetch('/api/guidelines/screen/' + screen, { headers: csrfHeaders() })
            .then(function(res) {
                console.log('[Onboarding] API status:', res.status);
                if (!res.ok) return null;
                return res.json();
            })
            .then(function(data) {
                console.log('[Onboarding] API data:', JSON.stringify(data).substring(0, 200));
                if (!data || !data.enabled) { console.log('[Onboarding] skip: not enabled'); return; }
                if (data.completed) { console.log('[Onboarding] skip: already completed'); return; }
                if (!data.screenEnabled) { console.log('[Onboarding] skip: screen not enabled'); return; }
                if (!data.steps || data.steps.length === 0) { console.log('[Onboarding] skip: no steps'); return; }

                state.data = data;
                createOverlayElements();
                showWelcome();
                console.log('[Onboarding] welcome modal shown');
            })
            .catch(function(err) {
                console.error('[Onboarding] error:', err);
            });
    }

    // Wait for page to fully load before starting onboarding
    if (document.readyState === 'complete') {
        setTimeout(init, 500);
    } else {
        window.addEventListener('load', function() {
            setTimeout(init, 500);
        });
    }
})();
